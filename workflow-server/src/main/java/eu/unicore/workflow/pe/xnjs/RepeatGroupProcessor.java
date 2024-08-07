package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.RepeatGroup;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * processes "repeat-until" loops
 *
 * @author schuller
 */
public class RepeatGroupProcessor extends GroupProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, RepeatGroupProcessor.class);

	public RepeatGroupProcessor(XNJS configuration) {
		super(configuration);
	}

	/**
	 * will look at the activities in the action's ActitivyGroup, and spawn
	 * subactions for dealing with those
	 */
	@Override
	protected void handleCreated() throws Exception {
		super.handleCreated();
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		RepeatGroup ag=(RepeatGroup)action.getAjd();
		logger.debug("Processing repeat group <{}> in workflow <{}> iteration <{}>", ag.getID(), ag.getWorkflowID(), getCurrentIteration());
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		addDeclarations(ag, vars);
		try{
			ag.getBody().getIterate().reset(vars);
		}catch(Exception ex){
			reportError("ITERATION_ERROR", Log.createFaultMessage("Iteration error", ex));
			throw ExecutionException.wrapped(ex);
		}
		submitAllEligibleActivities();
	}

	protected void submitAllEligibleActivities()throws Exception{
		List<String>subTasks=getOrCreateSubTasks();
		RepeatGroup ag=(RepeatGroup)action.getAjd();
		Iterate iterate=ag.getBody().getIterate();
		boolean haveMore=iterate.hasNext();
		if(!haveMore && (subTasks.size()==0)){
			setToDoneSuccessfully();
		}
		else{
			//process another iteration of the loop
			try(WorkflowContainer workflowInfo = PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID())){
				SubflowContainer attr=workflowInfo.findSubFlowAttributes(ag.getID());
				try{
					if(iterate.hasNext()){
						subTasks.add(submit(ag.getBody(), attr));
					}
				}catch(Exception ex){
					setToDoneAndFailed(Log.createFaultMessage("Exception occured", ex));
					throw ex;
				}
			}catch(Exception ex){
				throw ExecutionException.wrapped(ex);
			}
		}
	}


	@Override
	protected void handleRunning() throws Exception {
		logger.trace("Handle running for {}", action.getUUID());
		boolean subTasksStillRunning=false;

		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}

			RepeatGroup ag=(RepeatGroup)action.getAjd();
			List<String>subTasks=getOrCreateSubTasks();
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
					logger.trace("Sub-Action <{}> is <{}>", subActionID, ActionStatus.toString(status));
					if(ActionStatus.DONE!=status){
						subTasksStillRunning=true;
						continue subActionLoop;
					}
					else{
						action.setDirty();
						collectStatistics(sub);
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
					cleanupSubAction(sub);
					iterator.remove();
					}
				}
			}
			if(!subTasksStillRunning){
				Condition cond=ag.getCondition();
				cond.setIterate(ag.getIterate());
				ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
				cond.setProcessVariables(vars);
				if(!cond.evaluate()){
					submitAllEligibleActivities();
				}else{
					setToDoneSuccessfully();
				}
			}
		}catch(Exception ex){
			setToDoneAndFailed(Log.createFaultMessage("Error occurred", ex));
			throw ExecutionException.wrapped(ex);
		}
		if(subTasksStillRunning){
			sleep(5, TimeUnit.SECONDS);
		}

	}

}
