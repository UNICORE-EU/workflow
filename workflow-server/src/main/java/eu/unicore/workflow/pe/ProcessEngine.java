package eu.unicore.workflow.pe;

import java.util.Calendar;
import java.util.Map;

import eu.unicore.security.SecurityTokens;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.PEWorkflow;

/**
 * Main frontend for the process engine
 * 
 * @author schuller
 */
public interface ProcessEngine {

	/**
	 * process the supplied {@link ActivityGroup}
	 * 
	 * @param workflow - the workflow to process
	 * @param securityContext - the ws layer security context
	 */
	public void process(PEWorkflow workflow, SecurityTokens securityContext) throws Exception;

	
	/**
	 * process the supplied {@link ActivityGroup}
	 * 
	 * @param workflow - the workflow to process
	 * @param securityContext - the ws layer security context
	 * @param storageURL - the URL of the storage to use for workflow data
	 */
	public void process(PEWorkflow workflow, SecurityTokens securityContext, String storageURL, Calendar terminationTime) throws Exception;

	/**
	 * get the state of the workflow
	 * 
	 * @param workflowID - the ID of the workflow
	 */
	public ProcessState getProcessState(String workflowID) throws Exception;

	/**
	 * abort a workflow
	 * 
	 * @param workflowID - the ID of the workflow to abort
	 */
	public void abort(String workflowID) throws Exception;

	/**
	 * resume a workflow
	 * 
	 * @param workflowID - the ID of the workflow to abort
	 * @param params - resume parameters
	 */
	public void resume(String workflowID, Map<String,String>params) throws Exception;

}
