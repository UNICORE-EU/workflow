package eu.unicore.workflow;

import org.json.JSONObject;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Allows to submit workflows
 * 
 * @author schuller
 */
public class WorkflowFactoryClient extends BaseServiceClient {

	public WorkflowFactoryClient(String endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public WorkflowClient submitWorkflow(JSONObject job) throws Exception {
		return new WorkflowClient(bc.create(job), security, auth);
	}

	/**
	 * get the list of workflows
	 */
	public EnumerationClient getWorkflowList() throws Exception {
		return new EnumerationClient(endpoint, security, auth);
	}

}
