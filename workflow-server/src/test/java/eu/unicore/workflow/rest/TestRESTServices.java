package eu.unicore.workflow.rest;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.workflow.pe.PEConfig;

public class TestRESTServices extends WSSTestBase {

	@Test
	public void testRunDate()throws Exception{
		
		RESTClient client = createWorkflow();
		JSONObject wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		JSONObject in = loadJSON("src/test/resources/json/date1.json");
		HttpResponse res = client.post(in);
		client.checkError(res);
		
		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));

	}
	
	@Test
	public void testRunForEach()throws Exception{
		
		RESTClient client = createWorkflow();
		JSONObject wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		JSONObject in = loadJSON("src/test/resources/json/foreach.json");
		HttpResponse res = client.post(in);
		client.checkError(res);
		
		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));

	}
	
	@Test
	public void testRunTwoSteps()throws Exception{
		
		RESTClient client = createWorkflow("src/test/resources/json/twostep.json");
		JSONObject wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));

	}
	
	
	@Test
	public void testRunTwoStepWithOutputs()throws Exception{
		
		RESTClient client = createWorkflow("src/test/resources/json/two-with-outputs.json");
		JSONObject wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		String wfID = client.getURL().substring(client.getURL().lastIndexOf("/")+1);
		waitWhileRunning(client);

		wfProps = client.getJSON();
		System.out.println(wfProps.toString(2));
		
		// check location map
		Map<String,String> loc = PEConfig.getInstance().getLocationStore().read(wfID).getLocations();
		System.out.println(loc);
	}
	
	private RESTClient createWorkflow() throws Exception {
		return createWorkflow(null);
	}
	
	private RESTClient createWorkflow(String wfFileName) throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		RESTClient client = new RESTClient(url,kernel.getClientConfiguration());
		String resource  = url+"/workflows";
		client.setURL(resource);
		
		JSONObject wf = wfFileName==null ? 
				new JSONObject() : 
				new JSONObject(FileUtils.readFileToString(new File(wfFileName), "UTF-8"));
		wf.put("name","test123");
		wf.put("storageURL","https://somestorage");
		
		HttpResponse res = client.post(wf);
		String newWF = res.getFirstHeader("Location").getValue();
		System.out.println("Created new workflow "+newWF);
		EntityUtils.consumeQuietly(res.getEntity());
		client.setURL(newWF);
		return client;
	}
	
	
	
	protected JSONObject waitWhileRunning(RESTClient client) throws Exception {
		String status ="UNDEFINED";
		JSONObject wfProps = client.getJSON();
		int c=0;
		do{
			Thread.sleep(1000);
			wfProps = client.getJSON();
			status = wfProps.getString("status");
		}while(c<60 && ("RUNNING".equals(status) || "UNDEFINED".equals(status)));
		return wfProps;
	}
}
