package eu.unicore.workflow;

/**
 * Document available functions for evaluation in scripts and conditions
 *  
 * @author schuller
 */
public interface EvaluationFunctions {

	/**
	 * the format used to denote times : yyyy-MM-dd HH:mm
	 */
	public static final String DATE_FORMAT="yyyy-MM-dd HH:mm";
	
	/**
	 * check if the given time is later than now 
	 */
	public boolean after(String time) throws Exception;
	
	/**
	 * check if the given time is before now 
	 */
	public boolean before(String time) throws Exception;
	
	/**
	 * get the last known exit code of the given activity
	 * 
	 * @param activityID 
	 * @return the exit code
	 * @throws Exception in case the exit code can't be accessed or is not available
	 */
	public int exitCode(String activityID) throws Exception;
	
	/**
	 * check if the exit code matches a certain value
	 * 
	 * @param activityID
	 * @param compareTo
	 * @return <code>true</code> if the exit code matches
	 * @throws Exception in case the exit code can't be accessed or is not available
	 */
	public boolean exitCodeEquals(String activityID, int compareTo) throws Exception;
	
	/**
	 * check if the exit code does not equal a certain value
	 * 
	 * @param activityID
	 * @param compareTo
	 */
	public boolean exitCodeNotEquals(String activityID, int compareTo) throws Exception;
	
	/**
	 * check if a file exists. This can also be a global file, if the path starts
	 * with the logical filename prefix (i.e. "wf:")
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	public boolean fileExists(String activityID, String path) throws Exception;

	/**
	 * check if the length of a file is greater than zero. 
	 * This can also refer to a global file, if the path starts
	 * with "wf:"
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	public long fileLength(String activityID, String path) throws Exception;
	
	/**
	 * check if the length of a file is greater than zero. 
	 * This can also refer to a global file, if the path starts
	 * with "wf:"
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	public boolean fileLengthGreaterThanZero(String activityID, String path) throws Exception;

	/**
	 * read the named file from the 
	 * working directory of the given activity
	 * 
	 * @param activityID
	 * @param path - the file path
	 */
	public String fileContent(String activityID, String path) throws Exception;
	
}
