package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.util.TestActivity;

/**
 * Testing only: processes a single workflow activity<br/>
 * The processor randomizes the time that each activity takes  
 * to simulate a bit the real-life conditions in the workflow engine 
 * @author schuller
 */
@Ignore
public class TestingActivityProcessor extends ProcessorBase implements Constants{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, TestingActivityProcessor.class);

	public static final String ACTION_TYPE="TESTING";

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
		StringBuilder sb = new StringBuilder();
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		if(vars!=null){
			for(String s: vars.keySet()) {
				sb.append(s).append("=").append(String.valueOf(vars.get(s))).append(" ");
			}
		}
		logger.info("Processing activity <"+activity.getID()+"> in iteration <"
				+myIteration+">, state <"+sb+">");

		Validate.invoked(activity.getID());
		Validate.actionCreated(action.getUUID());
		action.setStatus(ActionStatus.RUNNING);
		if(activity.waitForExternalCallback()){
			action.setWaiting(true);
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
		TestActivity activity=(TestActivity)action.getAjd();
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
