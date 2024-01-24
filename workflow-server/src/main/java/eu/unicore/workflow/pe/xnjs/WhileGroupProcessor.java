package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.EvaluationException;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.WhileGroup;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;

/**
 * processes "do-while" loops
 *
 * @author schuller
 */
public class WhileGroupProcessor extends GroupProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, WhileGroupProcessor.class);

	public WhileGroupProcessor(XNJS configuration) {
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
		WhileGroup ag=(WhileGroup)action.getAjd();
		logger.debug("Processing WhileGroup <{}> in workflow <{}> iteration <{}>", ag.getID(), ag.getWorkflowID(), getCurrentIteration());
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		addDeclarations(ag, vars);
		try{
			ag.getBody().getIterate().reset(vars);
		}catch(Exception ex){
			reportError("ITERATION_ERROR", Log.createFaultMessage("Iteration error", ex));
			throw new ProcessingException(ex);
		}
		try{
			submitAllEligibleActivities();
		}catch(Exception ex){
			reportError("ERROR", Log.createFaultMessage("Error", ex));
			throw new ProcessingException(ex);
		}
	}

	protected void submitAllEligibleActivities()throws ProcessingException{
		WhileGroup ag=(WhileGroup)action.getAjd();
		Condition cond=ag.getCondition();
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		cond.setProcessVariables(vars);
		try{
			if(!cond.evaluate()){
				setToDoneSuccessfully();
				return;
			}
		}catch(EvaluationException ee){
			throw new ProcessingException(ee);
		}
		List<String>subTasks=getOrCreateSubTasks();
		
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
			//check substates ...
			List<String>subTasks = getOrCreateSubTasks();
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
				submitAllEligibleActivities();
			}
		}catch(Exception ex){
			setToDoneAndFailed(Log.createFaultMessage("Error occurred", ex));
			throw new ProcessingException(ex);
		}
		if(subTasksStillRunning){
			sleep(5, TimeUnit.SECONDS);
		}
	}

}
