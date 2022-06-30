package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

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

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, ForGroupProcessor.class);

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
		logger.info("Processing for-each group <{}> in workflow <{}> iteration <{}>",ag.getID(), ag.getWorkflowID(), getCurrentIteration());

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
		Iterate iterate=ag.getBody().getIterate();
		boolean haveMore=iterate.hasNext();
		if(!haveMore && (subTasks.size()==0)){
			action.addLogTrace("All iterations processed.");
			logger.info("ForGroup <{}>: All iterations processed.", action.getUUID());
			setToDoneSuccessfully();
		}
		else{
			//process some more iterations of the loop
			try(WorkflowContainer workflowInfo = PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID())){
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
					int maxConcurrentSystem = properties.getIntValue(WorkflowProperties.FOR_EACH_MAX_CONCURRENT_ACTIVITIES);
					int concurrentFromUser = ag.getMaxConcurrentActivities();
					if(concurrentFromUser<0) {
						concurrentFromUser = properties.getIntValue(WorkflowProperties.FOR_EACH_CONCURRENT_ACTIVITIES);
					}
					int maxConcurrent = Math.min(concurrentFromUser, maxConcurrentSystem);

					while(iterate.hasNext()){
						//limit the number of concurrent loop iterations
						if(subTasks.size()>=maxConcurrent){
							break;
						}
						workflowInfo.setDirty();
						subTasks.add(submit(ag.getBody(), attr));
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

	@Override
	protected void handleRunning() throws ProcessingException {
		logger.trace("Handle running for {}", action.getUUID());
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
					logger.trace("Sub-Action <{}> is {}", subActionID, ActionStatus.toString(status));
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

						// collect statistics from sub-activity
						Statistics subStats=sub.getProcessingContext().get(Statistics.class);
						getStatistics().addAll(subStats);

						
						//clean up the sub-action
						xnjs.get(Manager.class).destroy(subActionID, action.getClient());
						iterator.remove();
						//store activity status to global workflow info
						try(WorkflowContainer workflowInfo = PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID())){
							SubflowContainer attr = workflowInfo.findSubFlowAttributes(ag.getID());
							String iteration=(String)sub.getProcessingContext().get(PV_KEY_ITERATION);
							String subActivityID=((Activity)sub.getAjd()).getID();
							PEStatus stat=attr.getActivityStatus(subActivityID, iteration);
							stat.setActivityStatus(ActivityStatus.SUCCESS);
							workflowInfo.setDirty();
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
