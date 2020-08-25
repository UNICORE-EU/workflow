package eu.unicore.workflow;

import org.apache.http.HttpResponse;
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
		HttpResponse resp = bc.post(job);
		bc.checkError(resp);
		if(201 != resp.getStatusLine().getStatusCode()){
			throw new Exception("Unexpected return status: "+
					resp.getStatusLine().getStatusCode());
		}
		String url = resp.getFirstHeader("Location").getValue();
		Endpoint ep = endpoint.cloneTo(url);
		return new WorkflowClient(ep, security, auth);
	}
	
	/**
	 * get the list of workflows
	 */
	public EnumerationClient getWorkflowList() throws Exception {
		return new EnumerationClient(endpoint.cloneTo(endpoint.getUrl()), security, auth);
	}

}
