package eu.unicore.workflow.pe.xnjs;

public interface Constants {

	/**
	 * key for storing the current iteration value (a String) in the XNJS Action processing context
	 */
	public static final String PV_KEY_ITERATION="ITERATION_CURRENTVALUE";

	/**
	 * special workflow variable (can be used in job environment and file names) 
	 * holding the current iterator value
	 */
	public static final String VAR_KEY_CURRENT_TOTAL_ITERATION="CURRENT_TOTAL_ITERATOR";
	
}
