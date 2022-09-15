package eu.unicore.workflow.pe;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.utils.Pair;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.Constants;
import eu.unicore.workflow.EvaluationFunctions;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.model.EvaluationException;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;

/**
 * access and evaluate job and system parameters for use in variables and conditions
 * 
 * @author schuller
 */
public class Evaluator implements EvaluationFunctions {

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, Evaluator.class);

	private final String workflowID;

	private final String iteration;

	public Evaluator(String workflowID, String iteration){
		this.workflowID=workflowID;
		this.iteration=iteration;
	}
	
	public String getIteration() {
		return iteration;
	}

	private static final SimpleDateFormat dateFormat=new SimpleDateFormat(DATE_FORMAT);

	public synchronized String getFormatted(Calendar c){
		return dateFormat.format(c.getTime());
	}

	/**
	 * check if the given time is later than now 
	 */
	@Override
	public synchronized boolean after(String time) throws EvaluationException {
		try{
			Calendar timeCal=Calendar.getInstance();
			timeCal.setTime(dateFormat.parse(time));
			return Calendar.getInstance().after(timeCal);
		}catch(Exception ex){
			throw errorReport("n/a", "Can't parse date, format should be '"+DATE_FORMAT+"'", ex);
		}
	}

	/**
	 * check if the given time is before now 
	 */
	@Override
	public synchronized boolean before(String time) throws EvaluationException {
		try{
			Calendar timeCal=Calendar.getInstance();
			timeCal.setTime(dateFormat.parse(time));
			return Calendar.getInstance().before(timeCal);
		}catch(Exception ex){
			throw errorReport("n/a", "Can't parse date, format should be '"+DATE_FORMAT+"'", ex);
		}
	}

	/**
	 * get the last known exit code of the given activity
	 * 
	 * @param activityID 
	 * @return the exit code
	 * @throws Exception in case the exit code can't be accessed or is not available
	 */
	@Override
	public int exitCode(String activityID)throws EvaluationException {
		try{
			logger.debug("Getting exit code for activity <{}> in iteration <{}>", activityID, iteration);
			JobClient jc=getJobClient(activityID,iteration);
			Integer exitCode=jc.getExitCode();
			if(exitCode!=null){
				return exitCode.intValue();
			}
			else {
				throw new Exception("Exit code is null for job "+jc.getEndpoint().getUrl());
			}			
		}catch(Exception e){
			throw errorReport(activityID, "Evaluation error", e);
		}

	}

	/**
	 * check if the exit code matches a certain value
	 * 
	 * @param activityID
	 * @param compareTo
	 * @return <code>true</code> if the exit code matches
	 * @throws Exception in case the exit code can't be accessed or is not available
	 */
	@Override
	public boolean exitCodeEquals(String activityID, int compareTo)throws EvaluationException {
		return exitCode(activityID)==compareTo;
	}

	/**
	 * check if the exit code does not equal a certain value
	 * 
	 * @param activityID
	 * @param compareTo
	 */
	@Override
	public boolean exitCodeNotEquals(String activityID, int compareTo)throws EvaluationException {
		return exitCode(activityID)!=compareTo;
	}

	/**
	 * check if a file exists. This can also be a workflow file 
	 * if the path starts with "wf:"
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	@Override
	public boolean fileExists(String activityID, String path) throws EvaluationException {
		try {
			if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
				return logicalFileExists(path);
			}
			return uspaceFileExists(activityID, path);
		}catch(Exception ex) {
			throw errorReport(activityID, "Cannot check existence of <"+path+">", ex);
		}
	}


	/**
	 * check if the length of a file is greater than zero. 
	 * This can also refer to a Chemomentum global file, if the path starts
	 * with {@link Constants#LOGICAL_FILENAME_PREFIX} (i.e. "wf:")
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	@Override
	public long fileLength(String activityID, String path) throws EvaluationException {
		try {
			if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
				return logicalFileLength(activityID, path);
			}
			return uspaceFileLength(activityID, path);
		}catch(Exception ex) {
			throw errorReport(activityID, "Cannot check length of <"+path+">", ex);
		}
	}

	/**
	 * check if the length of a file is greater than zero. 
	 * This can also refer to a workflow file, if the path starts with "wf:"
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	@Override
	public boolean fileLengthGreaterThanZero(String activityID, String path) throws EvaluationException {
		try {
			if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
				return logicalFileLengthGreaterThanZero(activityID, path);
			}
			return uspaceFileLengthGreaterZero(activityID, path);
		}catch(Exception ex) {
			throw errorReport(activityID, "Cannot check length of <"+path+">", ex);
		}
	}

	@Override
	public String fileContent(String activityID, String path) throws EvaluationException {
		try {
			if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
				return logicalFileContent(activityID, path);
			}
			return uspaceFileContent(activityID, path);
		}catch(Exception ex) {
			throw errorReport(activityID, "Cannot get file content for <"+path+">", ex);
		}
	}

	protected String uspaceFileContent(String activityID, String path) throws Exception{
		JobClient jc = getJobClient(activityID,iteration);
		StorageClient sms = jc.getWorkingDirectory();
		HttpFileTransferClient ft = null;
		try {
			ft = (HttpFileTransferClient)sms.createExport(path, "BFT", null);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ft.readAllData(bos);
			return bos.toString();
		}
		finally{
			if(ft!=null)ft.delete();
		}
	}

	/**
	 * check if a file exists in the working directory belonging to an activity
	 * 
	 * @param activityID - the activity id
	 * @param path - the file path
	 */
	protected boolean uspaceFileExists(String activityID, String path) throws Exception {
		try{
			getFileProperties(activityID, path);
			return true;
		}catch(RESTException e){
			return false;
		}
	}

	/**
	 * returns the length of a file in uspace
	 * or <code>-1</code> if that file does not exist
	 */
	protected long uspaceFileLength(String activityID, String path) throws Exception{
		FileListEntry gft=getFileProperties(activityID, path);
		return gft.size;
	}

	protected boolean uspaceFileLengthGreaterZero(String activityID, String path) throws Exception {
		return uspaceFileLength(activityID, path)>0;
	}

	protected boolean logicalFileExists(String path) throws Exception {
		Locations locations = PEConfig.getInstance().getLocationStore().read(workflowID);
		return locations.getLocations().keySet().contains(path);
	}

	protected long logicalFileLength(String activityID, String path) throws Exception {
		Locations locations = PEConfig.getInstance().getLocationStore().read(workflowID);
		String location = locations.getLocations().get(path);
		if(location==null) throw new FileNotFoundException("Workflow file <"+path+"> not found");
		String[] tok = location.split("/files/",2);
		String url = tok[0];
		String file = tok[1];
		Kernel kernel = PEConfig.getInstance().getKernel();
		IClientConfiguration sp = kernel.getClientConfiguration().clone();
		StorageClient sms = new StorageClient(new Endpoint(url), sp, getAuth());
		return sms.stat(file).size;
	}

	protected String logicalFileContent(String activityID, String path) throws Exception {
		Pair<StorageClient, String> res = resolve(activityID, path);
		return download(res.getM1(), res.getM2());
	}	

	protected boolean logicalFileLengthGreaterThanZero(String activityID, String path) throws Exception {
		return logicalFileLength(activityID, path)>0;
	}

	private Pair<StorageClient, String> resolve(String activityID, String wfFile) throws Exception {
		Locations locations = PEConfig.getInstance().getLocationStore().read(workflowID);
		String location = locations.getLocations().get(wfFile);
		if(location==null) throw new FileNotFoundException("Workflow file <"+wfFile+"> not found");
		String[] tok = location.split("/files/",2);
		String url = tok[0];
		String file = tok[1];
		Kernel kernel = PEConfig.getInstance().getKernel();
		IClientConfiguration sp = kernel.getClientConfiguration().clone();
		return new Pair<>(new StorageClient(new Endpoint(url), sp, getAuth()), file);
	}

	private String download(StorageClient sms, String path) throws Exception {
		if(sms.stat(path).size>128*1024)throw new Exception("File too large.");
		HttpFileTransferClient ft = null;
		try {
			ft = (HttpFileTransferClient)sms.createExport(path, "BFT", null);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ft.readAllData(bos);
			return bos.toString();
		}
		finally{
			if(ft!=null)ft.delete();
		}
	}

	private JobClient getJobClient(String activityID, String iteration)throws Exception{
		String url = findJobReference(activityID, iteration);
		Kernel kernel = PEConfig.getInstance().getKernel();
		IClientConfiguration sp = kernel.getClientConfiguration().clone();
		return new JobClient(new Endpoint(url), sp, getAuth());
	}

	private IAuthCallback getAuth() throws Exception {
		Kernel kernel = PEConfig.getInstance().getKernel();
		String user = getUserDN();
		return new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
	}

	private String findJobReference(String activityID, String iteration)throws Exception{
		return findStatus(activityID,iteration).getJobURL();
	}

	private PEStatus findStatus(String activityID,String iteration)throws Exception{
		SubflowContainer wf=PEConfig.getInstance().getPersistence().read(workflowID);
		SubflowContainer ac=wf.findSubFlowContainingActivity(activityID);
		if(iteration!=null){
			return ac.getActivityStatus(activityID,iteration);
		}
		else{
			List<PEStatus> activityStatus=ac.getActivityStatus(activityID);
			if(activityStatus.size()>0){
				PEStatus res=activityStatus.get(activityStatus.size()-1);
				logger.debug("Found latest status for activity <{}>: {}", activityID, res);
				return res;
			}
		}
		return null;
	}  

	private String getUserDN()throws Exception {
		return PEConfig.getInstance().getPersistence().read(workflowID).getUserDN();
	}  

	private FileListEntry getFileProperties(String activityID, String path) throws Exception {
		return getJobClient(activityID,iteration).getWorkingDirectory().stat(path);
	}

	protected EvaluationException errorReport(String activityID, String message, Throwable cause) {
		String exc = Log.createFaultMessage(message, cause);
		logger.debug("For workflow <{}>, activity <{}> in iteration <{}> error occurred: ", workflowID, activityID, exc);
		return new EvaluationException("Error: "+exc+" for activity ID "+activityID+"> in iteration <"+iteration+">");
	}

}
