package eu.unicore.workflow.pe.util;

import java.util.Collection;

import eu.unicore.services.utils.Utilities;
import eu.unicore.workflow.pe.model.Activity;
 
/**
 * WA utilities
 * @author schuller
 *
 */
public class WorkAssignmentUtils { 

	public static final String SEP="/";
		
	
	/**
	 * encode some parameters into a XNJS action ID
	 *  
	 * @param workflowID
	 * @param activityID
	 * @param iteration
	 * 
	 * @return  a UUID (provided workflow ID is a UUID)
	 */
	public static String buildActionID(String workflowID, String activityID, String iteration){
		String uid = workflowID+SEP+activityID+SEP+iteration;
		if(uid.length()>255) {
			uid =  workflowID+SEP+Utilities.newUniqueID();
		}
		return uid;
	}
	
	
	/**
	 * encode some parameters into a workassignment ID 
	 * @param workflowID - the ID of the parent workflow 
	 * @param activityID - the ID of the underlying {@link Activity}
	 * @param iteration - the current iteration value 
	 * @param submissionCounter - the submission count for this workassignment
	 * @return a UUID (provided workflow ID is a UUID)
	 */
	public static String getEncodedWorkAssignmentID(String workflowID, String activityID, String iteration, String submissionCounter){
		return buildActionID(workflowID, activityID, iteration)+SEP+submissionCounter;
	}
	
	/**
	 * extract the workflow ID from a workassignment ID 
	 * @param workAssignmentID - the encoded ID
	 */
	public static String getWorkflowIDFromEncodedWAID(String workAssignmentID){
		return workAssignmentID.split("/")[0];
	}
	
	/**
	 * extract the Activity id from a workassignment ID 
	 * @param waID - the encoded ID
	 */
	public static String getActivityIDFromEncodedWAID(String waID){
		return waID.split(SEP)[1];
	}

	
	/**
	 * extract the XNJS Action UUID from a workassignment ID 
	 * @param waID - the encoded ID
	 */
	public static String getActionIDFromEncodedWAID(String waID){
		return waID.substring(0, waID.lastIndexOf(SEP));
	}

	/**
	 * extract the iteration id from a workassignment ID 
	 * @param workAssignmentID - the encoded ID
	 */
	public static String getIterationFromEncodedWAID(String workAssignmentID){
		return workAssignmentID.split(SEP)[2];
	}

	/**
	 * extract the "specification ID" (i.e. workflow activity ID) from a workassignment ID 
	 * @param workAssignmentID - the encoded ID
	 */
	public static String getSubmissionCounterFromEncodedWAID(String workAssignmentID){
		return workAssignmentID.split(SEP)[3];
	}

	public static String toCommaSeparatedList(Collection<String>list, String appendTo) {
		StringBuilder sb = new StringBuilder(appendTo);
		for(String s: list) {
			if(sb.length()>0)sb.append(",");
			sb.append(s);
		}
		return sb.toString();
	}
	
}
