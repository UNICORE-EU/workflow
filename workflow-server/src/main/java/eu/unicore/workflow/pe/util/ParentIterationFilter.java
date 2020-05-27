package eu.unicore.workflow.pe.util;

import eu.unicore.workflow.Constants;

import eu.unicore.workflow.pe.persistence.Filter;

public class ParentIterationFilter implements Filter {

	private final String prefix;
	
	public ParentIterationFilter(String prefix){
		this.prefix=prefix;
	}
	
	/**
	 * Accepts a value if it starts with the given prefix. 
	 * If the prefix is null, all values are accepted.
	 */
	public boolean accept(String value) {
		if(prefix==null || "".equals(prefix))return true;
		
		if(value.contains(Constants.ITERATION_ID_SEPERATOR)){
			return value.startsWith(prefix+Constants.ITERATION_ID_SEPERATOR);	
		}
		
		return value.equals(prefix);
	}

}
