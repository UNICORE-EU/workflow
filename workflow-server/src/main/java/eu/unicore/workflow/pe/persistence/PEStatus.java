package eu.unicore.workflow.pe.persistence;

import java.io.Serializable;

import eu.unicore.workflow.pe.model.ActivityStatus;

/**
 * stores status information for a workflow element (activities and transitions)
 * for one iteration
 * 
 * TODO track multiple submissions per iteration?
 * 
 * @author schuller
 */
public class PEStatus implements Serializable{

	private static final long serialVersionUID = 1L;

	private String errorDescription;
	
	private String errorCode;
	
	private String iterationValue;

	private String jobURL;
	
	private ActivityStatus activityStatus;

	private long startTime = 0;
	private long elapsedTime = -1;

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public ActivityStatus getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(ActivityStatus activityStatus) {
		this.activityStatus = activityStatus;
		if(ActivityStatus.RUNNING==activityStatus && startTime==0) {
			startTime = System.currentTimeMillis()/1000;
		}
		if(ActivityStatus.SUCCESS==activityStatus && startTime>0) {
			elapsedTime = System.currentTimeMillis()/1000 - startTime;
		}
	}

	public String getIteration() {
		return iterationValue;
	}

	public void setIteration(String iteration) {
		this.iterationValue = iteration;
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("[iteration=").append(iterationValue).append(" status=").append(activityStatus);
		if(errorCode!=null){
			sb.append(" errorCode=").append(errorCode);
		}
		if(errorDescription!=null){
			sb.append(" errorDescription=").append(errorDescription);
		}
		sb.append("]");
		return sb.toString();
	}

	public String getJobURL() {
		return jobURL;
	}

	public void setJobURL(String jobURL) {
		this.jobURL = jobURL;
	}
	
}
