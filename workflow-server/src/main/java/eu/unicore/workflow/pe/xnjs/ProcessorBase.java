package eu.unicore.workflow.pe.xnjs;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Kernel;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.ems.processors.DefaultProcessor;
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

/**
 * base class for workflow engine processors
 * 
 * @author schuller
 */
public class ProcessorBase extends DefaultProcessor implements Constants{

	protected static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, DefaultProcessor.class);
	
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
		return kernel.getClientConfiguration().clone();
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
				if(wfc==null){
					logger.error("No parent workflow found for activity <"+activityID+">");
					return;
				}
				SubflowContainer sfc=wfc.findSubFlowContainingActivity(activityID);
				if(sfc!=null){
					PEStatus status=sfc.getActivityStatus(activityID,iteration);
					status.setErrorCode(errorCode);
					status.setErrorDescription(errorDescription);
					status.setActivityStatus(ActivityStatus.FAILED);
					wfc.setDirty();
				}
				else{
					logger.warn("No status reporting possible for workflow <{}> activity <{}> in iteration <{}>",
							workflowID, activityID, iteration);
				}
			}
		}catch(Exception ex){
			Log.logException("Error while reporting error", ex, logger);
		}
	}

	/**
	 * set the action to "waiting" state and schedule it to wake up in N seconds
	 */
	protected void sendActionToSleep(int N){
		action.setWaiting(true);
		xnjs.getScheduledExecutor().schedule(new Runnable(){
			public void run(){
				final String actionID=action.getUUID();
				try{
					xnjs.get(InternalManager.class).handleEvent(new ContinueProcessingEvent(actionID));
				}catch(ExecutionException ee){
					Log.logException("Error sending continuation message for action <"+actionID+">", ee, logger);
				}
			}
		}, N, TimeUnit.SECONDS);
	}

	public void setAction(Action a){
		this.action=a;
	}
}
