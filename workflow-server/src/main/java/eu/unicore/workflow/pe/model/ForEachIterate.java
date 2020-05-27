package eu.unicore.workflow.pe.model;


public interface ForEachIterate extends Iterate {

	/**
	 * Workflow variable containing the current iterator value in a for-each group  
	 */
	public static final String PV_CURRENT_FOR_EACH_VALUE="CURRENT_ITERATOR_VALUE";
	
	/**
	 * Workflow variable containing the current iterator index in a for-each group  
	 */
	public static final String PV_CURRENT_FOR_EACH_INDEX="CURRENT_ITERATOR_INDEX";

	/**
	 * get the current index of the underlying value set
	 */
	public String getCurrentIndex();

}
