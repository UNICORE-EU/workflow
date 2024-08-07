package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;

/**
 * Testing only: processes a single workflow activity<br/>
 * The processor randomizes the time that each activity takes  
 * to simulate a bit the real-life conditions in the workflow engine 
 * @author schuller
 */
@Disabled
public class TestingActivityProcessor extends ProcessorBase implements Constants{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, TestingActivityProcessor.class);

	public static final String ACTION_TYPE="TESTING";

	public TestingActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws Exception {
		super.handleCreated();
		if(!isTopLevelWorkflowStillRunning()){
			setToDoneAndFailed("Parent workflow was aborted or failed");
			reportError("PARENT_FAILED","Parent aborted or failed.");
			return;
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
	protected void handlePostProcessing()throws Exception{
		if(!isTopLevelWorkflowStillRunning()){
			setToDoneAndFailed("Parent workflow was aborted or failed");
			reportError("PARENT_FAILED","Parent aborted or failed.");
			return;
		}
		String errorCode=(String)action.getProcessingContext().get(JSONExecutionActivityProcessor.LAST_ERROR_CODE);
		String errorDescription=(String)action.getProcessingContext().get(JSONExecutionActivityProcessor.LAST_ERROR_DESCRIPTION);
		setToDoneAndFailed("Failed: "+errorCode+" "+errorDescription);
	}

	@Override
	protected void handleRunning() throws Exception{
		if(!isTopLevelWorkflowStillRunning()){
			setToDoneAndFailed("Parent workflow was aborted or failed");
			reportError("PARENT_FAILED","Parent aborted or failed.");
			return;
		}
		TestActivity activity=(TestActivity)action.getAjd();
		if(activity.useCallback()){
			String workAssignmentID=action.getUUID()+"/someiteration";
			PEConfig.getInstance().getCallbackProcessor().handleCallback(activity.getWorkflowID(), workAssignmentID, "", true);
		}
		else{
			setToDoneSuccessfully();
		}
	}

}
