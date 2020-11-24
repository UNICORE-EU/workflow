package eu.unicore.workflow.pe.xnjs;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
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

	private static final Logger logger=Log.getLogger(Log.SERVICES,RepeatGroupProcessor.class);

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
		logger.info("Processing repeat group <"+ag.getID()+"> in workflow <"+ag.getWorkflowID()+"> iteration <"+getCurrentIteration()+">");

		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		addDeclarations(ag, vars);
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
		RepeatGroup ag=(RepeatGroup)action.getAjd();
		boolean dirty=false;
		Iterate iterate=ag.getBody().getIterate();
		boolean haveMore=iterate.hasNext();
		if(!haveMore && (subTasks.size()==0)){
			setToDoneSuccessfully();
		}
		else{

			//process another iteration of the loop

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
					if(iterate.hasNext()){
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

			RepeatGroup ag=(RepeatGroup)action.getAjd();
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
			sendActionToSleep(3);
		}

	}

}
