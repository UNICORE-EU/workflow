package eu.unicore.workflow.rest;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

public class RESTClient extends BaseClient {

	public RESTClient(String url, IClientConfiguration security,
			IAuthCallback authCallback) {
		super(url, security, authCallback);
	}

	public RESTClient(String url, IClientConfiguration security) {
		super(url, security);
	}

	public HttpResponse post(XmlObject xml) throws Exception {
		HttpPost post=new HttpPost(url);
		post.setHeader("Content-Type", MediaType.APPLICATION_XML);
		if(xml!=null)post.setEntity(new StringEntity(xml.toString(), ContentType.APPLICATION_XML));
		HttpResponse response = execute(post);
		return response;
	}

}
