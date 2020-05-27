package eu.unicore.workflow;

public interface Constants {
	/**
	 * String that is used to separate iteration counters in nested workflow loops 
	 * from each other.
	 */
	public static final String ITERATION_ID_SEPERATOR = ":::";
	
	/**
	 * Workflow variable that holds a String with iteration ids, separated by the {@link #ITERATION_ID_SEPERATOR}. 
	 * This represents the structure of nested loops. E.g. :::3:::1 would mean that we are in iteration 3 of the
	 * outer loop and iteration 1 of the inner loop. 
	 */
	public static final String CURRENT_TOTAL_ITERATOR = "${CURRENT_TOTAL_ITERATOR}";
	
	/**
	 * Files created by workflow jobs get stored at global storages. In order to retain
	 * the original paths of these files in the original job working directories, the path
	 * of these files on the global storages is determined to consist of the 
	 * corresponding work assignment id, followed by this separator, followed
	 * by the original path, e.g.
	 * 7890baeb-456a-4c30-abfa-213b4c1f72b9/Script No 1/1_0___output_files___/folder1/folder2/file
	 * ---------- workflow ID -------------/activity ID/iter. ID_resubmission ID ORIGINAL_PATH_SEPERATOR/---original path---
	 * --------------------- work assignment ID --------------------------------ORIGINAL_PATH_SEPERATOR/---original path---
	 */
	public static final String ORIGINAL_PATH_SEPERATOR = "___output_files___";
	
	
	/**
	 * URI "protocol" indicating a logical file name that needs to be resolved
	 * to one or more physical addresses for accessing the actual file.
	 */
	public static String LOGICAL_FILENAME_PREFIX="wf:";
}
