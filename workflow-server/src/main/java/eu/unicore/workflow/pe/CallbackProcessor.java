package eu.unicore.workflow.pe;

/**
 * Callback methods related to workassignment results
 */
public interface CallbackProcessor {
	
	/**
	 * @oaram wfID
	 * @param jobURL
	 * @param statusMessage
	 * @param success
	 */
	public void handleCallback(String wfID, String jobURL, String statusMessage, boolean success) ;
	

}