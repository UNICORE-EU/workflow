package eu.unicore.workflow;

import java.util.Map;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * manage a single job
 * 
 * @author schuller
 */
public class WorkflowClient extends BaseServiceClient {

	public static enum Status {
		UNDEFINED,

		RUNNING,

		SUCCESSFUL,

		ABORTED,

		FAILED,

		HELD,
	}

	public WorkflowClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public Status getStatus() throws Exception {
		return Status.valueOf(getProperties().getString("status"));
	}
	
	public boolean isFinished() throws Exception {
		Status s = getStatus();
		return Status.FAILED==s || Status.SUCCESSFUL==s || Status.ABORTED==s;
	}
	
	public void resume(Map<String,String> params) throws Exception {
		executeAction("continue", JSONUtil.asJSON(params));
	}
	
	public void abort() throws Exception {
		executeAction("abort", null);
	}
	
}
