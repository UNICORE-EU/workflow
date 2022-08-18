package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.RepeatGroup;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

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
	protected void handleCreated() throws ProcessingException {
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
			throw new ProcessingException(ex);
		}
		submitAllEligibleActivities();
	}

	protected void submitAllEligibleActivities()throws ProcessingException{
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
			throw new ProcessingException(ex);
		}
		if(subTasksStillRunning){
			sleep(5);
		}

	}

}
