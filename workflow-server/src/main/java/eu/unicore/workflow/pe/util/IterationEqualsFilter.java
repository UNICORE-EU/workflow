package eu.unicore.workflow.pe.util;

import eu.unicore.workflow.pe.persistence.Filter;

public class IterationEqualsFilter implements Filter {

	private final String prefix;
	
	public IterationEqualsFilter(String prefix){
		this.prefix=prefix;
	}
	
	/**
	 * Accepts a value if it starts with the given prefix. 
	 * If the prefix is null, all values are accepted.
	 */
	public boolean accept(String value) {
		if(prefix==null || "".equals(prefix))return true;
		
		return value.equals(prefix);
	}

}
