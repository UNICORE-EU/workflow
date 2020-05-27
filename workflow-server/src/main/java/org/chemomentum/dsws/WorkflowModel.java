package org.chemomentum.dsws;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.uas.impl.UASBaseModel;

public class WorkflowModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	String dialect;

	String storageURL;
	
	String submittedWorkflow;
	
	String workflowName;
	
	Calendar submissionTime;
	
	// variable names / types
	private final Map<String,String> declaredVariables = new HashMap<>();
	
	final List<String> jobURLs = new ArrayList<String>();

	public List<String> getJobURLs(){
		return jobURLs;
	}
	
	public void setSubmittedWorkflow(String wf){
		submittedWorkflow = wf;
	}
	
	public void setStorageURL(String url){
		storageURL = url;
	}

	/**
	 * @return the map of declared variables with their types. Never null.
	 */
	public Map<String, String> getDeclaredVariables() {
		return declaredVariables;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public void setSubmissionTime(Calendar submissionTime) {
		this.submissionTime = submissionTime;
	}

	public void setWorkflowName(String name) {
		this.workflowName = name;
	}

	public String getDialect() {
		return dialect;
	}

	public String getStorageURL() {
		return storageURL;
	}

	public String getSubmittedWorkflow() {
		return submittedWorkflow;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public Calendar getSubmissionTime() {
		return submissionTime;
	}

}
