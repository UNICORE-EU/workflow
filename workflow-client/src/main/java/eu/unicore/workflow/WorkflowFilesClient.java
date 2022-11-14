package eu.unicore.workflow;

import java.util.Map;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * helper to deal with Workflow files
 * 
 * @author schuller
 */
public class WorkflowFilesClient extends BaseServiceClient {

	public WorkflowFilesClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}
	
	public void register(Map<String,String> mappings) throws Exception {
		bc.put(JSONUtil.asJSON(mappings));
		}
	
	public Map<String,String> getMappings() throws Exception{
		return JSONUtil.asMap(getProperties());
	}
	
	public Map<String,String> getMappings(String query) throws Exception{
		bc.pushURL(bc.getURL()+"/"+query);
		try {
			return JSONUtil.asMap(getProperties());
		} finally {
			bc.popURL();
		}
	}
}
