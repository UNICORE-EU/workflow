package eu.unicore.workflow.pe.xnjs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.iterators.Iteration;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

/**
 * processes activity groups, i.e. both toplevel workflows and sub-workflows
 *
 * @author schuller
 */
public class ActivityGroupProcessor extends GroupProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, ActivityGroupProcessor.class);
	
	public ActivityGroupProcessor(XNJS configuration) {
		super(configuration);
	}

	/**
	 * will look at the activities in the action's ActivityGroup, and spawn
	 * subactions for dealing with those
	 */
	@Override
	protected void handleCreated() throws ProcessingException {
		super.handleCreated();
		ActivityGroup ag=(ActivityGroup)action.getAjd();
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		addDeclarations(ag,vars);
		
		ag.init(vars);
		if(ag.getID().equals(ag.getWorkflowID())){
			logger.info("Processing workflow <{}>", ag.getWorkflowID());
		}
		else{
			logger.info("Processing group <{}> in workflow <{}> iteration <{}>", ag.getID(), ag.getWorkflowID(), getCurrentIteration());
		}
		if(ag.isCoBrokerActivities()){
			action.setStatus(ActionStatus.PREPROCESSING);
			action.addLogTrace("Status set to PREPROCESSING, will co-broker all group activities");
			return;
		}
		else{
			startRunning();
		}
	}
	
	private void startRunning() throws ProcessingException{
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		submitAllEligibleActivities(true);
	}
	
	
	protected void performCoBrokering() throws ProcessingException{
		ActivityGroup ag=(ActivityGroup)action.getAjd();
		logger.info("Co-brokering activities for <{}>", ag.getID());
		// first collect all Job activities
		Map<String,JSONObject>jobs = new HashMap<>();
		addJobs(jobs, ag, false);
		try {
			// TODO - this is just a temporary thing
			String host = null;
			for(JSONObject j: jobs.values()) {
				host= j.optString("Site name", null);
				if(host!=null)break;
			}
			setupPreferredHost(jobs.values(), host);
			updateJobdescriptions(jobs, ag, false);
			startRunning();
		}
		catch(Exception ex){
			String msg=ex.getMessage();
			setToDoneAndFailed(msg);
		}
	}
	
	private void setupPreferredHost(Collection<JSONObject>jobs, String host) throws JSONException {
		for(JSONObject j: jobs){
			if(host!=null)j.put("Site name", host);
		}
	}

	private void addJobs(Map<String,JSONObject>addTo, ActivityGroup group, boolean recurse){
		for(Activity a: group.getActivities()){
			if(a instanceof ActivityGroup){
				addJobs(addTo, (ActivityGroup)a, recurse);
			}
			if(! (a instanceof JSONExecutionActivity))continue;
			JSONExecutionActivity jea=(JSONExecutionActivity)a;
			addTo.put(jea.getID(),jea.getJobDefinition());
		}
	}
	
	private void updateJobdescriptions(Map<String,JSONObject> updated, ActivityGroup group, boolean recurse){
		for(Activity a: group.getActivities()){
			if(a instanceof ActivityGroup){
				addJobs(updated, (ActivityGroup)a, recurse);
			}
			if(! (a instanceof JSONExecutionActivity))continue;
			
			JSONExecutionActivity jea=(JSONExecutionActivity)a;
			jea.setJobDefinition(updated.get(jea.getID()));
		}
	}
	
	protected void submitAllEligibleActivities(boolean subActionsStillRunning)throws ProcessingException{
		List<String>subTasks=getOrCreateSubTasks();
		ActivityGroup ag=(ActivityGroup)action.getAjd();
		List<Activity>activities=ag.getDueActivities();
		if(!subActionsStillRunning && activities.size()==0){
			action.addLogTrace("No more transitions left to follow.");
			logger.debug("ActivityGroup {}: No more transitions left to follow.", action.getUUID());
			setToDoneSuccessfully();
			return;
		}
		else{
			try(WorkflowContainer workflowInfo = PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID())){
				SubflowContainer attr=workflowInfo.findSubFlowAttributes(ag.getID());
				try{
					for(Activity a: activities){
						String id;
						if(a instanceof ActivityGroup){
							String loopIteratorName=((ActivityGroup)a).getLoopIteratorName();
							if(loopIteratorName!=null){
								Iterate iter=a.getIterate();
								if(iter instanceof Iteration){
									((Iteration) iter).setIteratorName(loopIteratorName);
								}
							}	
							id=submit((ActivityGroup)a,attr);
						}
						else {
							id=submit(a,attr);
						}
						subTasks.add(id);
					}
				}catch(Exception ex){
					setToDoneAndFailed(Log.createFaultMessage("Exception occured", ex));
					throw new ProcessingException(ex);
				}

			}catch(Exception ex){
				throw new ProcessingException(ex);
			}
		}
	}

	/**
	 * in PREPROCESSING, co-brokering is performed
	 */
	@Override
	protected void handlePreProcessing() throws ProcessingException {
		logger.trace("Handle pre-processing for {}", action.getUUID());
		performCoBrokering();
	}
	
	@Override
	protected void handleRunning() throws ProcessingException {
		logger.trace("Handle running for {}", action.getUUID());
		boolean stillRunning=false;
		
		boolean stopProcessingThisGroup=false;
		
		try{
			if(!isTopLevelWorkflowStillRunning()){
				String msg="Parent workflow was aborted or failed";
				setToDoneAndFailed(msg);
				reportError("PARENT_FAILED", msg);
				return;
			}

			ActivityGroup ag=(ActivityGroup)action.getAjd();
			//check substates ...
			List<String>subTasks=getOrCreateSubTasks();
			if(subTasks==null){
				throw new IllegalStateException("Could not find list of sub-tasks.");
			}

			Iterator<String>iterator=subTasks.iterator();
			
			subActionLoop: while(iterator.hasNext() && !stopProcessingThisGroup){
				String subActionID=iterator.next();
				Action sub=manager.getAction(subActionID);
				if(sub==null){
					String msg="WARNING: Can't find subaction with id "+subActionID;
					action.addLogTrace(msg);
					iterator.remove();
				}
				else{
					int status=sub.getStatus();
					logger.trace("Sub-Action <{}> is ", subActionID, ActionStatus.toString(status));
					
					if(ActionStatus.DONE!=status){
						stillRunning=true;
						continue subActionLoop;
					}

					if(ActionStatus.DONE==status){
						action.setDirty();
						collectStatistics(sub);
						String subActivityID=((Activity)sub.getAjd()).getID();
						Activity subActivity=ag.getActivity(subActivityID);

						//check result
						if(!sub.getResult().isSuccessful()){
							if(!shouldIgnoreFailure(sub)){
								subActivity.setStatus(ActivityStatus.FAILED);
								setToDoneAndFailed("Sub-action failed.");
								reportError("SubActivityFailed","Sub-activity failed");
								stopProcessingThisGroup=true;
							}
							else {
								action.addLogTrace("Sub-action "+subActionID+" failed, ignoring.");
								subActivity.setStatus(ActivityStatus.SUCCESS);
							}
						}
						else{
							subActivity.setStatus(ActivityStatus.SUCCESS);
						}
	
						if(!stopProcessingThisGroup){
							ProcessVariables pv=sub.getProcessingContext().get(ProcessVariables.class);
							//set follow-on activities to state READY so they can be submitted
							ag.activityDone(subActivity, pv);
							cleanupSubAction(sub);
							iterator.remove();
						}
					}
				}
			}
		}catch(Exception ex){
			setToDoneAndFailed(Log.createFaultMessage("Error occurred", ex));
			throw new ProcessingException(ex);
		}

		if(stopProcessingThisGroup)return;
		
		if(ActionStatus.DONE!=action.getStatus()){
			submitAllEligibleActivities(stillRunning);
		}
		if(stillRunning){
			sleep(10);
		}
	}

	protected boolean shouldIgnoreFailure(Action sub){
		if(sub.getAjd() instanceof JSONExecutionActivity){
			JSONExecutionActivity work = (JSONExecutionActivity)sub.getAjd();
			return work.isIgnoreFailure();
		}
		return false;
	}

	/**
	 * log usage for a toplevel workflow
	 */
	protected void logUsage(){
		if(!logger.isInfoEnabled())return;
		
		ActivityGroup ag=(ActivityGroup)action.getAjd();
		String wfID=ag.getWorkflowID();
		if(!ag.getID().equals(wfID)){
			// not a toplevel workflow
			return;
		}
		
		boolean success=action.getResult()!=null && action.getResult().isSuccessful();
		String client=action.getClient()!=null?action.getClient().getDistinguishedName():"n/a";
		Statistics stats=getStatistics();
		StringBuilder sb=new StringBuilder();
		sb.append("USAGE [").append(wfID).append("]");
		sb.append("[").append(client).append("]");
		sb.append("[").append(success? "SUCCESSFUL":"FAILED").append("]");
		sb.append("[Runtime: ").append(stats.getTotalRuntime()).append("secs.");
		sb.append(" Total jobs: ").append(stats.getTotalJobs());
		sb.append("]");
		logger.info(sb.toString());
	}
	
	@Override
	protected void setToDoneSuccessfully(){
		super.setToDoneSuccessfully();
		sendNotification(null);
		logUsage();
	}
	
	@Override
	protected void setToDoneAndFailed(String reason){
		super.setToDoneAndFailed(reason);
		sendNotification(reason);
		logUsage();
	}
	
	protected void sendNotification(String failureReason) {
		ActivityGroup ag = (ActivityGroup)action.getAjd();
		String wfID = ag.getWorkflowID();
		String url = ag.getNotificationURL();
		if(url==null)return;
		
		try{
			Kernel kernel = PEConfig.getInstance().getKernel();
			final JSONObject msg = new JSONObject();
			msg.put("status", String.valueOf(ActionStatus.toString(action.getStatus())));
			msg.put("statusMessage", action.getResult().toString()+
					(failureReason!=null? failureReason : ""));
			msg.put("href", kernel.getContainerProperties().getContainerURL()+"/rest/workflows/"+wfID);
			msg.put("group_id", ag.getID());
			WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(wfID);
			if(wfc==null){
				throw new Exception("Parent workflow information not found.");
			}
			final String user = wfc.getUserDN();
			final IAuthCallback auth = PEConfig.getInstance().getAuthCallback(user);
			IClientConfiguration security = kernel.getClientConfiguration().clone();
			final BaseClient bc = new BaseClient(url, security, auth);
			
			Callable<String>task = new Callable<String>() {
				@Override
				public String call() throws Exception {
					bc.postQuietly(msg);
					return "OK";
				}
			};
			String res = new TimeoutRunner<String>(task, kernel.getContainerProperties().getThreadingServices(), 30, TimeUnit.SECONDS).call();
			if(res==null)throw new TimeoutException("Timeout waiting for notification send/reply");
			
		}catch(Exception ex) {
			logger.warn("Could not send success/failure notification for workflow <{}> to <{}>", wfID, url);
		}
		
	}
	
}
