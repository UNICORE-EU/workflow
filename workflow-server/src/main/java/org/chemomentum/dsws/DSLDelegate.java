package org.chemomentum.dsws;

import java.util.Map;

import eu.unicore.security.SecurityTokens;

/**
 * Delegate for domain-specific processing in the workflow service<br/>
 * 
 * Delegates support a specific workflow dialect, that can be queried by {@link #getDialect()}.
 * 
 * @author schuller
 */
public interface DSLDelegate {

	/**
	 * add a new workflow instance
	 * 
	 * @param uniqueID - unique ID for this workflow instance
	 * @param workflow - the workflow description
	 * @param securityTokens
	 * @return an map containing attributes 
	 */
	public ConversionResult convertWorkflow(String uniqueID, Object workflow, SecurityTokens securityTokens);
	
	/**
	 * get the dialect understood by this delegate
	 */
	public String getDialect();
	
	/**
	 * get a representation of the workflow status suitable for clients
	 */
	public Map<String,Object> getStatus(String uniqueID) throws Exception;
}
