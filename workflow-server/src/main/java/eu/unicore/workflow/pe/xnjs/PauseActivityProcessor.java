package eu.unicore.workflow.pe.xnjs;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.model.PauseActivity;

/**
 * Processes PAUSE activities
 * 
 * @author schuller
 */
public class PauseActivityProcessor extends ProcessorBase{

	private static final Logger logger=Log.getLogger(Log.SERVICES,PauseActivityProcessor.class);

	public PauseActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		PauseActivity work=(PauseActivity )action.getAjd();
		String iteration=getCurrentIteration();
		long sleepTime = work.getSleepTime();
		logger.info("Entering PAUSE task <"+work.getID()+"> in iteration <"+iteration
				+">, will sleep for <"+sleepTime+"> seconds.");
		
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		if(vars==null){
			vars=new ProcessVariables();
			action.getProcessingContext().put(ProcessVariables.class,vars);
		}
		
		vars.put(VAR_KEY_CURRENT_TOTAL_ITERATION,iteration);
		action.setWaiting(true);
		
		scheduleWakeupCall(sleepTime);
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
		logger.debug("Handling running action <"+action.getUUID()+">");

		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}
			logger.info("Exiting PAUSE task <"+action.getUUID()+">");
				setToDoneSuccessfully();
				return;
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

}
