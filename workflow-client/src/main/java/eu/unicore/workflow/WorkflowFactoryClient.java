package eu.unicore.workflow;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Allows to submit workflows
 * 
 * @author schuller
 */
public class WorkflowFactoryClient extends BaseServiceClient {

	public WorkflowFactoryClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public WorkflowClient submitWorkflow(JSONObject job) throws Exception {
		String url = bc.create(job);
		return new WorkflowClient(endpoint.cloneTo(url), security, auth);
	}
	
	/**
	 * get the list of workflows
	 */
	public EnumerationClient getWorkflowList() throws Exception {
		return new EnumerationClient(endpoint.cloneTo(endpoint.getUrl()), security, auth);
	}

}
