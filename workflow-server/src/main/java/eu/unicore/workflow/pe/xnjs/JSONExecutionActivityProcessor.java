package eu.unicore.workflow.pe.xnjs;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.JobClient.Status;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.lookup.RandomSelection;
import eu.unicore.client.lookup.SiteSelectionStrategy;
import eu.unicore.client.lookup.TargetSystemFinder;
import eu.unicore.uas.json.Builder;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.StageOutProcessor;
import eu.unicore.workflow.pe.files.StagingPreprocessor;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.InsertVariablesFilter;
import eu.unicore.workflow.pe.util.WorkAssignmentUtils;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;

/**
 * Processes a single workflow activity<br/>
 * 
 * It will submit workassignments / jobs directly to UNICORE/X <br/>
 * 
 * TODO support external execution / service orchestrator
 * 
 * @author schuller
 */
public class JSONExecutionActivityProcessor extends ProcessorBase {

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, JSONExecutionActivityProcessor.class);

	public static final String LAST_ERROR_CODE = JSONExecutionActivityProcessor.class.getName()+"_LAST_ERROR_CODE";

	public static final String LAST_ERROR_DESCRIPTION = JSONExecutionActivityProcessor.class.getName()+"_LAST_ERROR_DESCRIPTION";

	public static final String SEND_INSTANT = JSONExecutionActivityProcessor.class.getName()+"_SEND";

	public static final String WD_REF = JSONExecutionActivityProcessor.class.getName()+"_workdir";

	public static final String USER = JSONExecutionActivityProcessor.class.getName()+"_user";

	public static final String EXPORTS = JSONExecutionActivityProcessor.class.getName()+"_exports";


	public JSONExecutionActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	public JSONExecutionActivity getWork() {
		return (JSONExecutionActivity)action.getAjd();
	}

	@Override
	protected void handleCreated() throws Exception {
		super.handleCreated();
		JSONExecutionActivity work = getWork();
		String iteration=getCurrentIteration();
		logger.debug("Start processing Job execution activity <{}> in iteration <{}>", work.getID(), iteration);
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		vars.put(VAR_KEY_CURRENT_TOTAL_ITERATION,iteration);
		try {
			submitJob();
			action.setStatus(ActionStatus.RUNNING);
			action.addLogTrace("Status set to RUNNING.");
			//callback will wake it up again...
			action.setWaiting(true);
			//set to wakeup after some seconds (in case we miss a callback)
			scheduleWakeupCall();
		}catch(Exception e){
			String msg = Log.createFaultMessage("Problem creating/sending job to UNICORE/X", e);
			action.setStatus(ActionStatus.POSTPROCESSING);
			action.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_DESCRIPTION, msg);
			action.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_CODE, "SUBMIT_FAILED");
		}
	}

	/**
	 * sets a timed task to continue processing the action.
	 */
	protected void scheduleWakeupCall(){
		final String id=action.getUUID();
		boolean useNotifications = !Boolean.parseBoolean(getWork()
				.getOption(JSONExecutionActivity.OPTION_NO_NOTIFICATIONS));
		int wakeUpPeriod = useNotifications? 
				properties.getStatusPollingInterval() : properties.getFastPollingInterval();
		xnjs.getScheduledExecutor().schedule(()->{
			try{
				xnjs.get(InternalManager.class).handleEvent(new ContinueProcessingEvent(id));
			}catch(Exception ex){
				Log.logException("Error continuing action <"+id+">", ex, logger);
			}
		}, wakeUpPeriod, TimeUnit.SECONDS);
	}

	private void submitJob() throws Exception {
		JSONExecutionActivity work = getWork();
		getStatistics().incrementJobs();
		String workflowID = work.getWorkflowID();
		work.incrementSubmissionCounter();

		ProcessVariables vars = action.getProcessingContext().get(ProcessVariables.class);
		JSONObject wa = new InsertVariablesFilter(vars).filter(work.getJobDefinition());

		WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(workflowID);
		if(wfc==null){
			setToDoneAndFailed("Parent workflow information not found.");
			return;
		}
		String user = wfc.getUserDN();

		// TODO?!
		//Calendar lifetime=wfc.getLifetime();
		
		if(!Boolean.parseBoolean(work.getOption(JSONExecutionActivity.OPTION_NO_NOTIFICATIONS))){
			String cbUrl = getBaseURL()+workflowID+"/actions/callback";
			wa.put("Notification", cbUrl);
		}

		JSONArray imports = wa.optJSONArray("Imports");
		if(imports!=null && imports.length()>0) {
			JSONArray filtered = new StagingPreprocessor(workflowID).processImports(imports);
			wa.put("Imports", filtered);
			logger.debug("Filtered stage ins for {}: {}", work.getID(), filtered.toString(2));
		}

		// since we replace stuff like iterations, variables in the job,
		// we need to store the version we have submitted, to be able
		// to correctly register the outputs later
		JSONArray exports = wa.optJSONArray("Exports");
		if(exports!=null && exports.length()>0) {
			JSONArray filtered = new StagingPreprocessor(workflowID).processExports(exports);
			wa.put("Exports", filtered);
			action.getProcessingContext().put(EXPORTS, exports.toString());
			logger.debug("Filtered stage outs for {}: {}", work.getID(), filtered.toString(2));
		}
		String jobURL = doSubmit(wa, user);
		storeJobURL(jobURL);
		action.getProcessingContext().put(SEND_INSTANT, System.currentTimeMillis());
	}

	public static SiteSelectionStrategy defaultStrategy = new RandomSelection();

	private String doSubmit(JSONObject job, String user)throws Exception {
		Builder builder = new Builder(job.toString(2));
		if(getWork().getBlacklist().size()>0) {
			String existingBL = builder.getProperty("blacklist", "");
			builder.setProperty("blacklist",
					WorkAssignmentUtils.toCommaSeparatedList(getWork().getBlacklist(), existingBL));
		}
		TargetSystemFinder f = new TargetSystemFinder(null);
		SiteClient sc = f.findTSS(
				PEConfig.getInstance().getRegistry(), 
				PEConfig.getInstance().getConfigProvider(), 
				PEConfig.getInstance().getAuthCallback(user), 
				builder, 
				defaultStrategy);
		JSONObject prefs = job.optJSONObject("User preferences");
		if(prefs!=null) {
			for(String attr: new String[]{"uid", "xlogin", "role",
					"group", "pgid", "supgids", "supplementaryGroups" }) {
				String value = prefs.optString(attr, null);
				if(value!=null)sc.getUserPreferences().put(attr,value);
			}
		}
		JobClient jc = sc.submitJob(job);
		String wd = jc.getLinkUrl("workingDirectory");
		action.getProcessingContext().put(WD_REF, wd);
		action.getProcessingContext().put(USER, user);
		action.setDirty();
		return jc.getEndpoint().getUrl();
	}

	private void storeJobURL(String jobURL) throws Exception {
		String workflowID = getWork().getWorkflowID();
		String activityID = getWork().getID();
		String iteration = getCurrentIteration();
		String jobID = jobURL.substring(jobURL.lastIndexOf("/")+1);

		try(WorkflowContainer wfc = PEConfig.getInstance().getPersistence().getForUpdate(workflowID)){
			wfc.getJobMap().put(jobID, action.getUUID());
			SubflowContainer sfc=wfc.findSubFlowContainingActivity(activityID);
			PEStatus status = sfc.getActivityStatus(activityID,iteration);
			status.setJobURL(jobURL);
		}
	}

	private String getBaseURL() {
		return PEConfig.getInstance().getKernel().getContainerProperties().getContainerURL()+"/rest/workflows/";
	}

	/**
	 * in RUNNING, the status of the current job is checked
	 */
	@Override
	protected void handleRunning()throws Exception{
		// check last send instant
		Long lastSend = (Long)action.getProcessingContext().get(SEND_INSTANT);
		if(lastSend!=null && lastSend+5000>System.currentTimeMillis()){
			return;
		}
		if(!isTopLevelWorkflowStillRunning()){
			setToDoneAndFailed("Parent workflow was aborted or failed");
			reportError("PARENT_FAILED","Parent aborted or failed.");
			return;
		}

		String err="running";
		Pair<Status, String>state=new Pair<>(Status.RUNNING, err);
		String jobURL = getCurrentJobURL();
		try{
			state = getJobStatus(jobURL);
		}catch(Exception e){
			String msg="Problem getting status for job "+jobURL;
			Log.logException(msg,e,logger);
		}
		Status status = state.getM1();

		switch(status) {
		case READY:
		case STAGINGIN:
		case QUEUED:
		case RUNNING:
		case STAGINGOUT:
			sleep(properties.getStatusPollingInterval(), TimeUnit.SECONDS);
			return;

		case SUCCESSFUL:
			action.setStatus(ActionStatus.DONE);
			action.setResult(new ActionResult(ActionResult.SUCCESSFUL));
			return;

		case FAILED:
		default:
			err="Job failed.";
		}
		setToPostprocessing(err, state.getM2());

	}

	private Pair<Status,String>getJobStatus(String jobUrl) throws Exception {
		String user = action.getProcessingContext().getAs(USER, String.class);
		JobClient jc = new JobClient(new Endpoint(jobUrl), 
				PEConfig.getInstance().getConfigProvider().getClientConfiguration(jobUrl),
				PEConfig.getInstance().getAuthCallback(user));
		Status s = jc.getStatus();
		return new Pair<>(s, "OK");
	}


	private void setToPostprocessing(String error, String errorCode){
		action.setStatus(ActionStatus.POSTPROCESSING);
		action.getProcessingContext().put(LAST_ERROR_DESCRIPTION, error);
		if(errorCode!=null)action.getProcessingContext().put(LAST_ERROR_CODE, errorCode);
	}

	/**
	 * POSTPROCESSING means the job execution is finished. 
	 * If successful, we set status accordingly and clean up.
	 * If it failed, we check whether to resubmit or not
	 */
	@Override
	protected void handlePostProcessing()throws Exception{
		if(!isTopLevelWorkflowStillRunning()){
			setToDoneAndFailed("Parent workflow was aborted or failed");
			reportError("PARENT_FAILED","Parent aborted or failed.");
			return;
		}
		if(action.getResult().isSuccessful()) {
			setToDoneSuccessfully();
		}
		else {
			handleResubmit();
			return;
		}
		try{
			// register outputs
			String expDef = action.getProcessingContext().getAs(EXPORTS, String.class);
			if(expDef!=null) {
				JSONArray exports = new JSONArray(expDef);
				if(exports!=null && exports.length()>0) {
					new StageOutProcessor(getParentWorkflowID(), 
							action.getProcessingContext().getAs(WD_REF, String.class),
							action.getProcessingContext().getAs(USER, String.class)).registerOutputs(exports);

				}
			}
		}catch(Exception ex) {
			String msg = Log.createFaultMessage("Could not register outputs", ex);
			setToDoneAndFailed(msg);
		}
	}

	private void handleResubmit()throws Exception {
		JSONExecutionActivity work = getWork();
		String errorCode = (String)action.getProcessingContext().get(LAST_ERROR_CODE);
		String errorDescription = (String)action.getProcessingContext().get(LAST_ERROR_DESCRIPTION);
		action.getProcessingContext().remove(LAST_ERROR_CODE);
		action.getProcessingContext().remove(LAST_ERROR_DESCRIPTION);
		logger.debug("Handling failure of <{}> error code was <{}>", work.getID(), errorCode);
		int submitted = work.getSubmissionCounter();

		int maxOption = properties.getResubmissionLimit();
		String maxOptionS = work.getOption(JSONExecutionActivity.OPTION_MAX_RESUBMITS);
		boolean userWantsResubmit = false;

		if(maxOptionS!=null){
			try{
				maxOption = Integer.parseInt(maxOptionS);
				userWantsResubmit = maxOption>0;
			}catch(NumberFormatException nfe){
				action.addLogTrace("WARNING: wrong number format set for option "+JSONExecutionActivity.OPTION_MAX_RESUBMITS);
			}
		}

		// user can set option to "0" intending NO resubmission
		int max = maxOption>0 ? Math.min(maxOption, getMaximumResubmitLimit()) : 0 ;

		//global flag
		boolean configNoResubmit = properties.isResubmitDisabled();

		//we can finally decide now. We can resubmit if:
		// 1. it is not switched off AND the user explicitly wants it
		// 2. the resubmit count is not exceeded
		boolean resubmit = (!configNoResubmit && userWantsResubmit) && max>submitted;

		if(resubmit){
			int attempt=submitted+1;
			logger.debug("Re-submitting <{}> this is attempt <{}> of <{}>", work.getID(), attempt, max);
			action.addLogTrace("Re-submitting, attempt <"+attempt+"> of <"+max+">");
			if(shouldAddBlacklistEntry(errorCode)){
				addToBlackList();
			}
			try {
				submitJob();
				action.setStatus(ActionStatus.RUNNING);
				action.setWaiting(true);
			}catch(Exception e){
				String msg = Log.createFaultMessage("Problem creating/sending job to UNICORE/X", e);
				action.setStatus(ActionStatus.POSTPROCESSING);
				action.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_DESCRIPTION, msg);
				action.getProcessingContext().put(JSONExecutionActivityProcessor.LAST_ERROR_CODE, "SUBMIT_FAILED");
				sleep(2, TimeUnit.SECONDS);
			}
		}
		else{
			logger.debug("Not resubmitting <{}>", work.getID());
			setToDoneAndFailed("Not resubmitting.");
			reportError(errorCode,errorDescription);
		}
	}

	/**
	 * decide whether to add a blacklist entry. This makes sense if the job has a chance
	 * to run successfully on a different target system
	 * 
	 * @param errorCode
	 */
	private boolean shouldAddBlacklistEntry(String errorCode){
		if(errorCode==null)return false;
		ErrorCode ect = ErrorCode.valueOf(errorCode);
		if(ect==null)return false;
		switch(ect){

		case SECURITY_ERROR: 
			return true;

		default: return false;

		}
	}

	private String getCurrentJobURL() throws Exception {
		JSONExecutionActivity work = getWork();
		WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(work.getWorkflowID());
		if(wfc==null){
			throw new IllegalStateException("No parent workflow found for activity <"+work.getID()+">");
		}
		PEStatus status=wfc.findSubFlowContainingActivity(work.getID()).getActivityStatus(work.getID(),getCurrentIteration());
		if(status!=null){
			return status.getJobURL();
		}else throw new IllegalStateException("No status found for activity <"+work.getID()+">");

	}

	private void addToBlackList(){
		JSONExecutionActivity work = getWork();
		try{
			//first, find current submission host
			WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(work.getWorkflowID());
			if(wfc==null){
				logger.debug("No parent workflow found for activity <{}>", work.getID());
				return;
			}
			PEStatus status=wfc.findSubFlowContainingActivity(work.getID()).getActivityStatus(work.getID(),getCurrentIteration());
			if(status!=null){
				String url = status.getJobURL();
				if(url!=null){
					url = url.split("/rest/")[0];
					work.addToBlackList(url);
				}
			}
		}catch(Exception ex){
			Log.logException("Problem adding blacklist entry for activity <"+work.getID(), ex, logger);
		}
	}

	public int getMaximumResubmitLimit(){
		return properties.getResubmissionLimit();
	}

	public static enum ErrorCode {
		OK,
		SUBMIT_FAILED,
		FILE_IMPORT_FAILED,
		FILE_EXPORT_FAILED,
		JOB_FAILED,
		SECURITY_ERROR,
		OTHER,
	}

}
