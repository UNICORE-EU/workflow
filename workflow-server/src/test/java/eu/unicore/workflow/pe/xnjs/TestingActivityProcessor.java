package eu.unicore.workflow.pe.xnjs;

import java.util.Random;

import org.apache.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.util.TestActivity;

/**
 * Testing only: processes a single workflow activity<br/>
 * The processor randomizes the time that each activity takes  
 * to simulate a bit the real-life conditions in the workflow engine 
 * @author schuller
 */
public class TestingActivityProcessor extends ProcessorBase implements Constants{

	private static final Logger logger=Log.getLogger(Log.SERVICES,TestingActivityProcessor.class);

	public static final String ACTION_TYPE="TESTING";

	private final static Random rand = new Random();

	public TestingActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		super.handleCreated();
		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
		
		getStatistics().incrementJobs();
		action.setStatus(ActionStatus.RUNNING);
		TestActivity activity=(TestActivity)action.getAjd();
		
		String myIteration=(String)action.getProcessingContext().get(PV_KEY_ITERATION);
		int n=activity.getDelay()>-1? activity.getDelay() : rand.nextInt(500);
		StringBuilder sb = new StringBuilder();
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		if(vars!=null){
			for(String s: vars.keySet()) {
				sb.append(s).append("=").append(String.valueOf(vars.get(s))).append(" ");
			}
		}
		logger.info("Processing activity <"+activity.getID()+"> in iteration <"
				+myIteration+">, waiting <"+n+"> millis, state <"+sb+">");

		Validate.invoked(activity.getID());
		Validate.actionCreated(action.getUUID());
		action.setStatus(ActionStatus.RUNNING);
		if(activity.waitForExternalCallback()){
			action.setWaiting(true);
		}
		else{
			//compute wait time
			Long waitUntil=System.currentTimeMillis()+n;
			action.getProcessingContext().put("wait",waitUntil);
		}
	}

	@Override
	protected void handlePostProcessing()throws ProcessingException{
		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}
			String errorCode=(String)action.getProcessingContext().get(JSONExecutionActivityProcessor.LAST_ERROR_CODE);
			String errorDescription=(String)action.getProcessingContext().get(JSONExecutionActivityProcessor.LAST_ERROR_DESCRIPTION);
			setToDoneAndFailed("Failed: "+errorCode+" "+errorDescription);
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

	@Override
	protected void handleRunning() throws ProcessingException{
		try{
			if(!isTopLevelWorkflowStillRunning()){
				setToDoneAndFailed("Parent workflow was aborted or failed");
				reportError("PARENT_FAILED","Parent aborted or failed.");
				return;
			}
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
		Long waitUntil=(Long)action.getProcessingContext().get("wait");
		TestActivity activity=(TestActivity)action.getAjd();
		if(System.currentTimeMillis()>waitUntil){
			if(activity.useCallback()){
				String workAssignmentID=action.getUUID()+"/someiteration";
				try{
					PEConfig.getInstance().getCallbackProcessor().handleCallback(activity.getWorkflowID(), workAssignmentID, "", true);
				}catch(Exception e){
					throw new ProcessingException(e);
				}
			}
			else{
				setToDoneSuccessfully();
			}
		}
	}

}
