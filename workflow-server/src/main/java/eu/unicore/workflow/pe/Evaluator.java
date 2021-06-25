package eu.unicore.workflow.pe;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import eu.unicore.workflow.EvaluationFunctions;
import eu.unicore.workflow.Constants;

import de.fzj.unicore.persist.PersistenceException;
import eu.unicore.services.Kernel;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.pe.model.EvaluationException;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

/**
 * access and evaluate job and system parameters for use in variables and conditions
 * 
 * @author schuller
 */
public class Evaluator implements EvaluationFunctions {

	private static final Logger logger=Log.getLogger(Log.SERVICES,Evaluator.class);

	private final String workflowID;

	private final String iteration;

	public Evaluator(String workflowID, String iteration){
		this.workflowID=workflowID;
		this.iteration=iteration;
	}
	
	@Override
	public boolean eval(boolean c){
		return c;
	}

	private static final SimpleDateFormat dateFormat=new SimpleDateFormat(DATE_FORMAT);
	
	protected synchronized String getFormatted(Calendar c){
		return dateFormat.format(c.getTime());
	}
	
	/**
	 * check if the given time is later than now 
	 */
	@Override
	public synchronized boolean after(String time){
		try{
			Calendar timeCal=Calendar.getInstance();
			timeCal.setTime(dateFormat.parse(time));
			return Calendar.getInstance().after(timeCal);
		}catch(Exception ex){
			throw new IllegalArgumentException("Can't parse date, format should be '"+DATE_FORMAT+"'");
		}
	}
	
	/**
	 * check if the given time is before now 
	 */
	@Override
	public synchronized boolean before(String time){
		try{
			Calendar timeCal=Calendar.getInstance();
			timeCal.setTime(dateFormat.parse(time));
			return Calendar.getInstance().before(timeCal);
		}catch(Exception ex){
			throw new IllegalArgumentException("Can't parse date, format should be '"+DATE_FORMAT+"'");
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
	public int exitCode(String activityID)throws Exception{
		try{
			if(logger.isDebugEnabled()){
				logger.debug("Getting exit code for activity <"+activityID+"> in iteration <"+iteration+">");
			}
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
	public boolean exitCodeEquals(String activityID, int compareTo)throws Exception{
		return exitCode(activityID)==compareTo;
	}

	/**
	 * check if the exit code does not equal a certain value
	 * 
	 * @param activityID
	 * @param compareTo
	 */
	@Override
	public boolean exitCodeNotEquals(String activityID, int compareTo)throws Exception{
		return exitCode(activityID)!=compareTo;
	}

	/**
	 * check if a file exists. This can also be a Chemomentum global file, if the path starts
	 * with the logical filename prefix (i.e. "c9m")
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	@Override
	public boolean fileExists(String activityID, String path) throws Exception {
		if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
			return logicalFileExists(path);
		}
		return uspaceFileExists(activityID, path);
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
	public long fileLength(String activityID, String path) throws Exception {
		if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
			throw new IllegalArgumentException("not implemented");
		}
		return uspaceFileLength(activityID, path);
	}

	/**
	 * check if the length of a file is greater than zero. 
	 * This can also refer to a Chemomentum global file, if the path starts
	 * with {@link C9MCommonConstants#LOGICAL_FILENAME_PREFIX} (i.e. "c9m:")
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	@Override
	public boolean fileLengthGreaterThanZero(String activityID, String path) throws Exception {
		if(path.startsWith(Constants.LOGICAL_FILENAME_PREFIX)){
			return logicalFileLengthGreaterThanZero(activityID, path);
		}
		return uspaceFileLengthGreaterZero(activityID, path);
	}

	@Override
	public String fileContent(String activityID, String path) throws Exception{
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
	protected boolean uspaceFileExists(String activityID, String path) throws EvaluationException {
		try{
			FileListEntry gft = getFileProperties(activityID, path);
			return gft!=null;
		}catch(Exception e){
			throw errorReport(activityID, "Evaluation error", e);
		}
	}

	/**
	 * returns the length of a file in uspace
	 * or <code>-1</code> if that file does not exist
	 */
	protected long uspaceFileLength(String activityID, String path) throws EvaluationException{
		try{
		    FileListEntry gft=getFileProperties(activityID, path);
		    if(gft==null)return -1;
			return gft.size;
		}catch(Exception e){
			throw errorReport(activityID, "Can't access uspace", e);
		}
	}
	
	protected boolean uspaceFileLengthGreaterZero(String activityID, String path) throws EvaluationException {
		return uspaceFileLength(activityID, path)>0;
	}

	//TODO check if logical file exists
	protected boolean logicalFileExists(String path){
		return true;
	}
	
	//TODO check logical file length
	protected boolean logicalFileLengthGreaterThanZero(String activityID, String path){
		return true;
	}

	protected JobClient getJobClient(String activityID, String iteration)throws Exception{
		String url = findJobReference(activityID, iteration);
		Kernel kernel = PEConfig.getInstance().getKernel();
		IClientConfiguration sp = kernel.getClientConfiguration().clone();
		String user = getUserDN();
		IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		return new JobClient(new Endpoint(url), sp, auth);
	}
	
	protected String findJobReference(String activityID, String iteration)throws TimeoutException,PersistenceException{
		PEStatus wa=findStatus(activityID,iteration);
		return wa.getJobURL();
	}

	protected PEStatus findStatus(String activityID,String iteration)throws TimeoutException,PersistenceException{
		SubflowContainer wf=PEConfig.getInstance().getPersistence().read(workflowID);
		SubflowContainer ac=wf.findSubFlowContainingActivity(activityID);
		if(iteration!=null){
			return ac.getActivityStatus(activityID,iteration);
		}
		else{
			List<PEStatus> activityStatus=ac.getActivityStatus(activityID);
			if(activityStatus.size()>0){
				PEStatus res=activityStatus.get(activityStatus.size()-1);
				if(logger.isDebugEnabled()){
					logger.debug("Found latest status for activity <"+activityID+">: "+res);
				}
				return res;
			}
		}
		return null;
	}  

	protected String getUserDN()throws PersistenceException, TimeoutException{
		WorkflowContainer attr=PEConfig.getInstance().getPersistence().read(workflowID);
		return attr.getUserDN();
	}  

	protected FileListEntry getFileProperties(String activityID, String path) throws EvaluationException {
		try{
			JobClient jc = getJobClient(activityID,iteration);
			StorageClient sms = jc.getWorkingDirectory();
			return sms.stat(path);
		}catch(Exception e){
			return null;
		}
	}
	
	protected EvaluationException errorReport(String activityID, String message, Throwable cause) {
		String exc = Log.createFaultMessage(message, cause);
		if(logger.isDebugEnabled()){
			logger.debug("For workflow <"+workflowID+">, activity <"+activityID+"> in iteration <"+iteration+"> error occurred: "+exc);
		}
		return new EvaluationException("Error: "+exc+" for activity ID "+activityID+"> in iteration <"+iteration+">");
	}
	
	public String getIteration(){
		return iteration;
	}

}
