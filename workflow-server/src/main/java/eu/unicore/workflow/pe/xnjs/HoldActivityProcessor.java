package eu.unicore.workflow.pe.xnjs;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.ModelBase;
import eu.unicore.workflow.pe.model.util.VariableUtil;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

/**
 * Processes HOLD activities
 * 
 * @author schuller
 */
public class HoldActivityProcessor extends ProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, HoldActivityProcessor.class);

	private static final String _exit_at = "__EXIT_AT";
	
	public HoldActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		HoldActivity work=(HoldActivity )action.getAjd();
		String iteration=getCurrentIteration();
		logger.debug("Workflow <{}> entering hold task <{}> in iteration <{}>", work.getWorkflowID(), work.getID(), iteration);
		long sleepTime = work.getSleepTime();
		if(sleepTime>0) {
			action.getProcessingContext().put(_exit_at, 
					Long.valueOf(System.currentTimeMillis()+1000*sleepTime));
		}
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		try {
			setToHold();
		}
		catch(Exception ex){
			String reason=Log.createFaultMessage("Could not hold workflow", ex);
			setToDoneAndFailed(reason);
			return;
		}
		vars.put(VAR_KEY_CURRENT_TOTAL_ITERATION,iteration);
		action.setWaiting(true);
		scheduleWakeupCall(5);
	}

	/**
	 * sets a timed task to continue processing the action.
	 */
	protected void scheduleWakeupCall(long seconds){
		final String id=action.getUUID();
		Runnable r=new Runnable(){
			public void run(){
				try{
					ContinueProcessingEvent event=new ContinueProcessingEvent(id);
					xnjs.get(InternalManager.class).handleEvent(event);
				}catch(Exception ex){
					Log.logException("Error continuing action <"+id+">", ex, logger);
				}
			}
		};
		xnjs.getScheduledExecutor().schedule(r, seconds, TimeUnit.SECONDS);
	}

	/**
	 * in state RUNNING, it is checked whether the condition to 
	 * exit the HOLD task is fulfilled 
	 */
	@Override
	protected void handleRunning()throws ProcessingException{
		logger.debug("Handling running action <{}>", action.getUUID());
		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}
			Pair<Boolean, Map<String,String>>resume=isHeld(getParentWorkflowID(),getActivityID());
			boolean exit = false;
			if(!resume.getM1()){
				logger.debug("Exiting HOLD task <{}>", action.getUUID());
				Map<String,String>resumeParams = resume.getM2();
				if(resumeParams!=null){
					try{
						updateProcessVariables(resumeParams);
					}catch(Exception e){
						String msg = Log.createFaultMessage("Error updating variable(s)", e);
						setToDoneAndFailed(msg);
						reportError("EvaluationError",msg);
						return;
					}
				}
				exit = true;
			}
			else {
				// check if we have a sleep interval set and it has passed
				Long wakeUp = (Long)action.getProcessingContext().get(_exit_at);
				if(wakeUp!=null && wakeUp<System.currentTimeMillis()) {
					logger.debug("Exiting HOLD task <{}> after it's waiting time has passed.", action.getUUID());
					exit = true;
				}
			}
			if(exit) {
				setToDoneSuccessfully();
				return;
			}
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
		action.setWaiting(true);
		action.setDirty();
		scheduleWakeupCall(5);
	}

	private void updateProcessVariables(Map<String,String>resumeParams){
		logger.debug("Updating <{}> variable(s) for <{}>", resumeParams.size(), action.getUUID());
		ProcessVariables vars = action.getProcessingContext().get(ProcessVariables.class);
		for(String name : resumeParams.keySet()){
			Object current = vars.get(name);
			String value = resumeParams.get(name);
			if(current == null){
				throw new IllegalArgumentException("No variable named '"+name+"'");
			}
			else{
				vars.put(name,VariableUtil.update(current, value));
				vars.markModified(name);
			}
		}
	}

	/**
	 * checks if the group containing this activity is set on hold
	 * @throws PersistenceException
	 */
	public static Pair<Boolean, Map<String,String>> isHeld(String workflowID, String activityID) throws PersistenceException{
		WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(workflowID);
		SubflowContainer sfc=wfc.findSubFlowContainingActivity(activityID);
		Pair<Boolean, Map<String,String>>res = new Pair<>();
		if(sfc!=null){
			if(sfc.isHeld()){
				res.setM1(Boolean.TRUE);
			}
			else{
				res.setM1(Boolean.FALSE);
				res.setM2(sfc.getResumeParameters());
			}
		}
		return res;
	}

	/**
	 * set the innermost group containing this activty on hold
	 * @throws PersistenceException
	 * @throws InterruptedException
	 */
	public void setToHold() throws PersistenceException, InterruptedException{
		ModelBase ag=(ModelBase)action.getAjd();
		String workflowID=ag.getWorkflowID();
		try(WorkflowContainer wfc=PEConfig.getInstance().getPersistence().getForUpdate(workflowID)){
			SubflowContainer sfc = wfc.findSubFlowContainingActivity(ag.getID());
			sfc.hold();	
		}
	}
}
