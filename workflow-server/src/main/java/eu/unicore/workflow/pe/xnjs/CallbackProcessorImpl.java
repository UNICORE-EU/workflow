package eu.unicore.workflow.pe.xnjs;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.CallbackProcessor;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

public class CallbackProcessorImpl implements CallbackProcessor{

	private static final Logger logger=Log.getLogger(Log.SERVICES,CallbackProcessorImpl.class);

	private final XNJS xnjs;
	private final Kernel kernel;

	private final ExecutorService es;

	public CallbackProcessorImpl(XNJS xnjs){
		this.xnjs = xnjs;
		kernel = xnjs.get(Kernel.class);
		es= kernel != null ? 
				kernel.getContainerProperties().getThreadingServices().getExecutorService():
				xnjs.getScheduledExecutor();
	}

	@Override
	public void handleCallback(String wfID, String jobURL, final String statusMessage, boolean success) {
		Runnable r=new Runnable(){
			public void run(){
				try{
					String jobID = jobURL.substring(jobURL.lastIndexOf("/")+1);
					WorkflowContainer wfc=PEConfig.getInstance().getPersistence().getForUpdate(wfID);
					if(wfc==null){
						logger.info("No parent workflow found for job <"+jobURL+">");
						return;
					}
					String actionID = wfc.getJobMap().get(jobID);
					if(actionID==null){
						logger.info("No action found for job <"+jobURL+">");
						return;
					}
					try{ 
						ContinueProcessingEvent cpe=new ContinueProcessingEvent(actionID){
							public void callback(final Action a){
								a.setStatus(ActionStatus.POSTPROCESSING);
								if(success) {
									a.setResult(new ActionResult(ActionResult.SUCCESSFUL));
								}
								else {
									a.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_DESCRIPTION, statusMessage);
									a.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_CODE, "JOB_FAILED");
									
								}
							}
						};
						xnjs.get(InternalManager.class).handleEvent(cpe);
					}
					finally{
						if(wfc!=null)PEConfig.getInstance().getPersistence().write(wfc);
					}
				}catch(Exception ex){
					Log.logException("Error processing callback for workflow <"+wfID+" > from job <"+jobURL+">",ex,logger);
				}
			}
		};
		es.execute(r);
	}
	
	
}
