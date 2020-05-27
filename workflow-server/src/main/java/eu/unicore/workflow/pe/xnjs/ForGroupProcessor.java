package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.ForGroup;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

/**
 * processes "for-each" loops
 *
 * @author schuller
 */
public class ForGroupProcessor extends GroupProcessorBase{

	private static final Logger logger=Log.getLogger(Log.SERVICES,ForGroupProcessor.class);

	public ForGroupProcessor(XNJS configuration) {
		super(configuration);
	}

	/**
	 * will look at the activities in the action's ActitivyGroup, and spawn
	 * subactions for dealing with those
	 */
	@Override
	protected void handleCreated() throws ProcessingException {
		super.handleCreated();
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		ForGroup ag=(ForGroup)action.getAjd();
		logger.info("Processing for-each group <"+ag.getID()+"> in workflow <"+ag.getWorkflowID()+"> iteration <"+getCurrentIteration()+">");

		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		try{
			ag.getBody().getIterate().reset(vars);
		}catch(Exception ex){
			reportError("ITERATION_ERROR", Log.createFaultMessage("Iteration error", ex));
			throw new ProcessingException(ex);
		}

		if(vars==null){
			vars=new ProcessVariables();
			action.getProcessingContext().put(ProcessVariables.class,vars);
		}
		submitAllEligibleActivities();
	}

	protected void submitAllEligibleActivities()throws ProcessingException{
		List<String>subTasks=getOrCreateSubTasks();
		ForGroup ag=(ForGroup)action.getAjd();
		boolean dirty=false;
		Iterate iterate=ag.getBody().getIterate();
		boolean haveMore=iterate.hasNext();
		if(!haveMore && (subTasks.size()==0)){
			action.addLogTrace("All iterations processed.");
			logger.info("ForGroup "+action.getUUID()+": All iterations processed.");
			setToDoneSuccessfully();
		}
		else{

			//process some more iterations of the loop

			WorkflowContainer workflowInfo=null;
			try{
				workflowInfo=PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID());
				if(workflowInfo==null){
					String msg="No workflow info for <"+ag.getWorkflowID()+">";
					logger.debug(msg);
					setToDoneAndFailed(msg);
					return;
				}
				SubflowContainer attr=workflowInfo.findSubFlowAttributes(ag.getID());
				if(attr==null)throw new PersistenceException("Persistent information about <"+ag.getID()+"> is missing");

				try{
					//get the system limit for the maximum concurrent activities
					int maxConcurrentSystem=properties.getIntValue(WorkflowProperties.FOR_EACH_MAX_CONCURRENT_ACTIVITIES);
					int maxConcurrentFromUser=ag.getMaxConcurrentActivities();
					//the maximum concurrent iterations specified by the user must not exceed the system limit
					int maxConcurrent=Math.min(maxConcurrentFromUser, maxConcurrentSystem);

					while(iterate.hasNext()){
						//limit the number of concurrent loop iterations
						if(subTasks.size()>=maxConcurrent){
							if(logger.isDebugEnabled()){
								logger.debug("Limit of <"+maxConcurrent+"> concurrent loop iterations reached, not submitting new actions");
							}
							break;
						}
						Activity a=ag.getBody();
						dirty=true;
						String id;

						if(a instanceof ActivityGroup){
							ActivityGroup grp=(ActivityGroup)a;
							id=submit(grp,attr);
						}
						else { //TODO to simplify, we might force that loop body is a group
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
			finally{
				if(workflowInfo!=null){
					try{
						if(dirty){
							PEConfig.getInstance().getPersistence().write(workflowInfo);
							action.setDirty();
						}
						else{
							PEConfig.getInstance().getPersistence().unlock(workflowInfo);
						}
					}catch(Exception ex){
						throw new ProcessingException(ex);
					}
				}
			}
		}
	}

	@Override
	protected void handleRunning() throws ProcessingException {
		if(logger.isTraceEnabled())logger.trace("Handle running for "+action.getUUID());
		boolean subTasksStillRunning=false;

		try{

			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}

			ForGroup ag=(ForGroup)action.getAjd();
			//check substates ...
			List<String>subTasks=getOrCreateSubTasks();
			if(subTasks==null){
				throw new IllegalStateException("Could not find list of sub-tasks.");
			}

			WorkflowContainer workflowInfo=null;
			SubflowContainer attr;

			Iterator<String>iterator=subTasks.iterator();
			subActionLoop: while(iterator.hasNext()){
				String subActionID=iterator.next();
				Action sub=manager.getAction(subActionID);
				if(sub==null){
					String msg="INTERNAL ERROR: Can't find subaction with id "+subActionID;
					action.addLogTrace(msg);
					throw new IllegalStateException(msg);
				}
				else{
					int status=sub.getStatus();
					if(logger.isTraceEnabled()){
						logger.trace("Sub-Action <"+subActionID+"> is "+ActionStatus.toString(status));
					}

					//check status

					if(ActionStatus.DONE!=status){
						subTasksStillRunning=true;
						continue subActionLoop;
					}

					if(ActionStatus.DONE==status){
						action.setDirty();
						//check result
						if(!sub.getResult().isSuccessful()){
							if(!shouldIgnoreFailure(sub)){
								setToDoneAndFailed("Sub-action failed.");
								reportError("CHILD_FAILED","A task was aborted or failed.");
								return;
							}
							else{
								action.addLogTrace("Sub-action failed, ignoring");
							}
						}
						//copy results
						ProcessVariables pv=sub.getProcessingContext().get(ProcessVariables.class);
						if(pv!=null){
							//TODO what about overwrite of existing values ...
							action.getProcessingContext().get(ProcessVariables.class).putAll(pv);
						}

						String subActivityID=((Activity)sub.getAjd()).getID();

						//clean up the sub-action
						xnjs.get(Manager.class).destroy(subActionID, action.getClient());
						iterator.remove();
						//store activity status to global workflow info
						try{
							workflowInfo=PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID());
							attr=workflowInfo.findSubFlowAttributes(ag.getID());
							String iteration=(String)sub.getProcessingContext().get(PV_KEY_ITERATION);
							PEStatus stat=attr.getActivityStatus(subActivityID, iteration);
							stat.setActivityStatus(ActivityStatus.SUCCESS);
						}finally{
							if(workflowInfo!=null){
								try{
									PEConfig.getInstance().getPersistence().write(workflowInfo);
								}catch(Exception ex){
									throw new ProcessingException(ex);
								}
							}
						}
					}
				}
			}
		}catch(Exception ex){
			setToDoneAndFailed(Log.createFaultMessage("Error occurred", ex));
			throw new ProcessingException(ex);
		}
		if(subTasksStillRunning){
			sendActionToSleep(3);
		}
		submitAllEligibleActivities();
	}

}
