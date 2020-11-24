package eu.unicore.workflow.rest;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;
import eu.unicore.workflow.pe.PEConfig;

public class TestRESTServices extends WSSTestBase {

	@Test
	public void testRunDate()throws Exception{
		
		WorkflowClient client = createWorkflow("src/test/resources/json/date1.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));

	}
	
	@Test
	public void testRunForEach()throws Exception{
		
		WorkflowClient client = createWorkflow("src/test/resources/json/foreach.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));

	}
	
	@Test
	public void testRunTwoSteps()throws Exception{
		
		WorkflowClient client = createWorkflow("src/test/resources/json/twostep.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));

	}
	
	
	@Test
	public void testRunTwoStepWithOutputs()throws Exception{
		
		WorkflowClient client = createWorkflow("src/test/resources/json/two-with-outputs.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		String wfURL = client.getEndpoint().getUrl();
		String wfID = wfURL.substring(wfURL.lastIndexOf("/")+1);
		// check location map
		Map<String,String> loc = PEConfig.getInstance().getLocationStore().read(wfID).getLocations();
		System.out.println(loc);
	}
	
	
	@Test
	public void testRunCatInput()throws Exception{
		FileUtils.forceMkdir(new File("target/data/WORK"));
		FileUtils.write(new File("target/data/WORK/input.txt"), "test123", "UTF-8");
		WorkflowClient client = createWorkflow("src/test/resources/json/cat-input.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		waitWhileRunning(client);

		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		
		String wfURL = client.getEndpoint().getUrl();
		String wfID = wfURL.substring(wfURL.lastIndexOf("/")+1);
		// check location map
		Map<String,String> loc = PEConfig.getInstance().getLocationStore().read(wfID).getLocations();
		System.out.println(loc);
		Assert.assertNotNull(loc.get("wf:infile"));
		
		// via REST API
		BaseServiceClient fileList = client.getFileList();
		JSONObject files = fileList.getProperties();
		System.out.println(files.toString(2));
		Assert.assertNotNull(files.getString("wf:infile"));
		
	}
	
	private WorkflowClient createWorkflow(String wfFileName) throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/workflows";
		WorkflowFactoryClient client = new WorkflowFactoryClient(new Endpoint(url),kernel.getClientConfiguration(),null);
		
		JSONObject wf = wfFileName==null ? 
				new JSONObject() : 
				new JSONObject(FileUtils.readFileToString(new File(wfFileName), "UTF-8"));
		try{
			wf.put("name", wfFileName.substring(wfFileName.lastIndexOf("/")+1));
		}catch(Exception ex) {
			wf.put("name", "n/a");
		}
		wf.put("storageURL","https://somestorage");
		
		return client.submitWorkflow(wf);
	}
	
	
	
	protected JSONObject waitWhileRunning(WorkflowClient client) throws Exception {
		String status ="UNDEFINED";
		JSONObject wfProps = client.getProperties();
		int c=0;
		do{
			Thread.sleep(1000);
			wfProps = client.getProperties();
			status = wfProps.getString("status");
		}while(c<60 && ("RUNNING".equals(status) || "UNDEFINED".equals(status)));
		return wfProps;
	}
}
