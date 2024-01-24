package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.PersistenceException;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.ProcessState;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.ModelBase;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;
import eu.unicore.xnjs.ems.processors.DefaultProcessor;

/**
 * base class for workflow engine processors
 * 
 * @author schuller
 */
public abstract class ProcessorBase extends DefaultProcessor implements Constants{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, ProcessorBase.class);
	
	protected WorkflowProperties properties;
	
	public ProcessorBase(XNJS configuration) {
		super(configuration);
		properties = configuration.get(WorkflowProperties.class);
	}

	protected Statistics getStatistics(){
		Statistics stats=action.getProcessingContext().get(Statistics.class);
		if(stats==null){
			stats=new Statistics(action.getUUID());
			action.getProcessingContext().put(Statistics.class, stats);
		}
		action.setDirty();
		return stats;
	}
	
	@Override
	protected void handleCreated() throws ProcessingException {
		getStatistics();
	}
	
	
	protected String getCurrentIteration(){
		return (String)action.getProcessingContext().get(PV_KEY_ITERATION);
	}
	
	protected String getActivityName(){
		return action.getAjd().getClass().getSimpleName();
	}
	
	/**
	 * check whether the parent workflow is still running
	 * @return <code>true</code> if parent workflow is running
	 * @throws Exception
	 */
	protected boolean isTopLevelWorkflowStillRunning()throws Exception{
		ModelBase ag=(ModelBase)action.getAjd();
		String workflowID=ag.getWorkflowID();
		ProcessState ps=PEConfig.getInstance().getProcessEngine().getProcessState(workflowID);
		return ProcessState.State.RUNNING.equals(ps.getState()) || ProcessState.State.HELD.equals(ps.getState()) ;
	}
	
	protected void setToDoneSuccessfully(){
		action.setStatus(ActionStatus.DONE);
		action.addLogTrace("Status set to DONE.");
		action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"Success.",0));
		action.addLogTrace("Result: Success.");
		logger.info("{} {} SUCCESSFUL", getActivityName(), action.getUUID());
	}

	protected void setToDoneAndFailed(String reason){
		action.fail();
		logger.info("{} {} FAILED. {}", getActivityName(), action.getUUID(), reason);
	}

	protected String getParentWorkflowID(){
		ModelBase ag=(ModelBase)action.getAjd();
		return ag.getWorkflowID();
	}

	protected String getActivityID(){
		ModelBase ag=(ModelBase)action.getAjd();
		return ag.getID();
	}

	protected IClientConfiguration getSecurityConfig()throws PersistenceException{
		Kernel kernel = xnjs.get(Kernel.class);
		if(kernel==null)return null;
		return kernel.getClientConfiguration();
	}
	
	/**
	 * store an error description in the persistent status for the activity currently processed
	 * 
	 * @param errorCode
	 * @param errorDescription
	 */
	protected void reportError(String errorCode,String errorDescription){
		try{
			String activityID = getActivityID();
			String workflowID = getParentWorkflowID();
			if(workflowID.equals(activityID))return;
			String iteration=getCurrentIteration();
			logger.debug("Reporting error: <{}> for activity <{}> in iteration <{}>", errorDescription, activityID, iteration);
			try(WorkflowContainer wfc = PEConfig.getInstance().getPersistence().getForUpdate(workflowID)){
				SubflowContainer sfc=wfc.findSubFlowContainingActivity(activityID);
				PEStatus status=sfc.getActivityStatus(activityID,iteration);
				status.setErrorCode(errorCode);
				status.setErrorDescription(errorDescription);
				status.setActivityStatus(ActivityStatus.FAILED);
			}
		}catch(Exception ex){
			Log.logException("Error while reporting error", ex, logger);
		}
	}

	public void setAction(Action a){
		this.action=a;
	}
}
