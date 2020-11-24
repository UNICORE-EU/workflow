package org.chemomentum.dsws;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.messaging.Message;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.ProcessState;
import eu.unicore.workflow.pe.ProcessState.State;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * a USE resource representing a workflow instance
 * 
 * @author schuller
 */
public class WorkflowInstance extends BaseResourceImpl {

	public static final String INITPARAM_DIALECT = "dialect";

	public static final String INITPARAM_PARENTID = "processid";

	public static final String INITPARAM_SUBMITTED_WORKFLOW = "submitted_wf";

	public static final String INITPARAM_ATTRIBUTES = "attributes";

	public static final String INITPARAM_WORKFLOW_NAME = "workflowname";

	public static final String INITPARAM_STORAGE_EPR = "storageEPR";

	private static final Logger logger = Log.getLogger(Log.SERVICES,
			WorkflowInstance.class);

	private ProcessState processState = null;

	public WorkflowInstance() {
		super();

	}

	@Override
	public WorkflowModel getModel() {
		return (WorkflowModel) model;
	}

	@Override
	public void initialise(InitParameters initMap)
			throws Exception {
		if (model == null) {
			model = new WorkflowModel();
		}
		super.initialise(initMap);

		WorkflowInitParameters wfInit = (WorkflowInitParameters)initMap;
		WorkflowModel model = getModel();
		model.setParentUID(wfInit.parentUUID);
		model.setParentServiceName(WorkflowFactoryHomeImpl.SERVICE_NAME);
		model.workflowName = wfInit.workflowName;

		model.setStorageURL(wfInit.storageURL);

		model.submissionTime = Calendar.getInstance();
		
		String[] tags = wfInit.initialTags;
		if(tags!=null && tags.length>0){
			model.getTags().addAll(Arrays.asList(tags));
		}
		
		Locations loc = new Locations();
		loc.setWorkflowID(getUniqueID());
		PEConfig.getInstance().getLocationStore().write(loc);
		
	}


	public ConversionResult submit(String dialect, Object wf, String storageURL)
			throws Exception {
		WorkflowModel model = getModel();
		ConversionResult conversionResult = null;
		model.dialect = dialect;
		if(storageURL!=null)model.setStorageURL(storageURL);

		conversionResult = convert(dialect, wf);

		model.getDeclaredVariables().putAll(conversionResult.getDeclaredVariables());
		model.setSubmittedWorkflow(String.valueOf(wf));

		if (conversionResult.hasConversionErrors()) {
			logger.info("Submitted workflow <"
					+ getUniqueID()
					+ "> contains errors, not submitting to process engine.");
		} else {
			Calendar tt = getHome().getTerminationTime(getUniqueID());
			startProcessing(conversionResult, tt);

		}
		return conversionResult;
	}

	protected ConversionResult convert(String dialect, Object wf) {
		WorkflowModel model = getModel();
		DSLDelegate del = WorkflowFactoryImpl.getDelegate(model.dialect);
		if (del == null) {
			throw new IllegalArgumentException("Dialect <" + model.dialect
					+ "> not understood");
		}
		return del.addNewWorkflow(getUniqueID(), wf, getSecurityTokens());
	}


	protected void startProcessing(ConversionResult conversionResult, Calendar terminationTime)
			throws Exception {
		logger.info("Start processing of workflow <" + getUniqueID() + ">");
		PEWorkflow peWorkflow = conversionResult.getConvertedWorkflow();
		PEConfig.getInstance()
		.getProcessEngine()
		.process(peWorkflow, getSecurityTokens(), getModel().storageURL, terminationTime);
	}

	public void doResume(Map<String, String> params) throws Exception {
		PEConfig.getInstance().getProcessEngine().resume(getUniqueID(), params);
	}

	// check if resume is possible
	public boolean canResume() {
		updateProcessState();
		return State.HELD.equals(processState.getState());
	}

	public void doAbort() throws Exception {
		if(!canAbort())return;
		PEConfig.getInstance().getProcessEngine().abort(getUniqueID());
		initiateCleanup(false);
	}

	// check if abort is possible
	public boolean canAbort() {
		updateProcessState();
		return State.RUNNING.equals(processState.getState())
				|| State.HELD.equals(processState.getState());
	}

	@Override
	public void destroy() {
		try {
			doAbort();
		} catch (Exception e) {
			Log.logException("Workflow not aborted.", e, logger);
		}
		try {
			Message m = new Message("deleted:" + getUniqueID());
			kernel.getMessaging().getChannel(getModel().getParentUID())
			.publish(m);
		} catch (Exception e) {
			Log.logException("Could not send internal message.", e, logger);
		}
		// asynchronously delete jobs and storage
		initiateCleanup(true);
		super.destroy();
	}

	/**
	 * Cleanup. Will asynchronously abort / destroy jobs and clean up the
	 * storage. If the supplied parameter is <code>true</code>, workflow storage
	 * and the jobs will be destroyed, if not configured otherwise. If the
	 * parameter is <code>false</code>, jobs will be aborted.
	 * 
	 * @param destroy - if <code>true</code>, it is a destroy, otherwise an abort operation
	 */
	private void initiateCleanup(final boolean destroy) {
		try {
			final WorkflowContainer wfc = PEConfig.getInstance()
					.getPersistence().read(getUniqueID());
			if (wfc == null)return;
			
			final IClientConfiguration sec = kernel.getClientConfiguration().clone();
			String user = wfc.getUserDN();
			IAuthCallback auth = PEConfig.getInstance().getAuthCallback(user);
			
			WorkflowProperties wp = kernel.getAttribute(WorkflowProperties.class);

			if (destroy && wp.isStorageCleanup()) {
				try {

					String url = getModel().getStorageURL();
					if (url != null && !url.toLowerCase().endsWith("home")) {
						StorageClient sc = new StorageClient(new Endpoint(url), sec, auth);
						sc.delete();
					}
				} catch (Exception ex) {
					logger.info("Could not remove storage for workflow "
							+ getUniqueID());
				}
			}

			final Collection<String> jobs = wfc.collectJobs();
			final boolean jobCleanup = wp.isJobsCleanup();
			Runnable r = new Runnable() {
				public void run() {
					if (destroy) {
						if (jobCleanup)
							logger.info("Will delete <" + jobs.size()
							+ "> jobs from workflow " + getUniqueID());
					} else {
						logger.info("Will abort <" + jobs.size()
						+ "> jobs from workflow " + getUniqueID());
					}
					String operation = destroy ? "destroy" : "abort";
					for (String job : jobs) {
						try {
							if (logger.isDebugEnabled()) {
								logger.debug("Will " + operation + " job: "+ job);
							}
							JobClient b = new JobClient(new Endpoint(job), sec, auth);
							if (destroy) {
								if (jobCleanup)
									b.delete();
							} else {
								b.abort();
							}
						} catch (Exception e) {
							Log.logException("Could not " + operation
									+ " job.", e, logger);
						}
					}

					if (destroy) {
						try {
							PEConfig.getInstance().getPersistence().remove(getUniqueID());
						} catch (Exception e) {
							Log.logException(
									"Could not delete persistent data for <"
											+ getUniqueID() + ">", e, logger);
						}
					}
				}
			};
			// execute asynchronously
			kernel.getContainerProperties().getThreadingServices().getExecutorService().execute(r);
			// more?

		} catch (Exception e) {
			Log.logException("Could not cleanup workflow <" + getUniqueID()
			+ ">", e);
		}
	}

	public Map<String,String> getVariableValues() {
		Map<String,String> varNames = getModel().getDeclaredVariables();
		ProcessVariables vars = getProcessState().getVariables();
		Map<String,String> parameters= new HashMap<>();
		// avoid null values
		for(String name: varNames.keySet()){
			Object value = vars.get(name);
			if(value == null) continue;
			parameters.put(name, String.valueOf(value));
		}
		return parameters;
	}

	/**
	 * update and get the process state
	 */
	public ProcessState getProcessState() {
		updateProcessState();
		return processState;
	}

	private void updateProcessState() {
		try {
			processState = PEConfig.getInstance().getProcessEngine()
					.getProcessState(getUniqueID());
		} catch (Exception e) {
			Log.logException("Can't get process state for <"
					+ getUniqueID() + ">", e, logger);
		}
	}

}
