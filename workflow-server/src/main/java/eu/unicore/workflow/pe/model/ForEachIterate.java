package eu.unicore.workflow.pe.model;


public interface ForEachIterate extends Iterate {

	/**
	 * Workflow variable containing the current iterator index in a for-each group  
	 */
	public static final String PV_CURRENT_FOR_EACH_INDEX="_CURRENT_ITERATOR_INDEX";

	/**
	 * List of file names separated by ";"
	 * This avoids the need to declare large numbers of variables and allows for
	 * the automatic evaluation of the lists within a job. 
	 */
	public static final String PV_ORIGINAL_FILENAMES = "_ORIGINAL_FILENAMES";
	
	/**
	 * Holds individual original file name variable. 
	 * When there are more than one chunk, this will have an "_N" appended, with
	 * "N" being the index of the file within the current chunk
	 */
	public static final String PV_ORIGINAL_FILENAME = "_ORIGINAL_FILENAME";
	
	/**
	 * get the current index of the underlying value set
	 */
	public String getCurrentIndex();

}
