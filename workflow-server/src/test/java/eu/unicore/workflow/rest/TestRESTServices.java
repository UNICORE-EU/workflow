package eu.unicore.workflow.rest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowClient.Status;
import eu.unicore.workflow.WorkflowFactoryClient;
import eu.unicore.workflow.WorkflowFilesClient;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.xnjs.Statistics;
import eu.unicore.xnjs.ems.InternalManager;

public class TestRESTServices extends WSSTestBase {

	@Test
	public void testBase()throws Exception{
		WorkflowFactoryClient client = getFactoryClient();
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
	}
	
	@Test
	public void testRunDate()throws Exception{
		String wfFileName = "src/test/resources/json/date1.json";
		JSONObject wf = new JSONObject(FileUtils.readFileToString(new File(wfFileName), "UTF-8"));
		wf.put("notification", kernel.getContainerProperties().getContainerURL()+"/rest/callbacks");
		WorkflowClient client = createWorkflow(wf);
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		assertEquals(1, wfProps.getJSONArray("tags").length());
		assertEquals(1, client.getJobList().getUrls(0, 100).size());
		client.abort();
		client.delete();
	}
	
	@Test
	public void testRunForEach()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/foreach.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		client.delete();
	}
	
	@Test
	public void testRunTwoSteps()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/twostep.json");
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		client.delete();
	}
	
	@Test
	public void testHoldContinue()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/hold-with-variables.json");
		JSONObject wfProps = client.getProperties();
		int c=0;
		do {
			Thread.sleep(1000);
			c++;
		}while(Status.HELD!=client.getStatus() && c<20);
		Map<String,String>params = new HashMap<>();
		params.put("COUNTER", "456");
		client.resume(params);
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		assert "456".equals(wfProps.getJSONObject("parameters").getString("COUNTER"));
		client.delete();
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
		Assert.assertNotNull(loc.get("wf:date1/out"));
		WorkflowFilesClient fileList = client.getFileList();
		fileList.setUpdateInterval(-1);
		Map<String,String>mappings = fileList.getMappings();
		System.out.println(mappings);
		Assert.assertNotNull(mappings.get("wf:date1/out"));
		// register
		Map<String,String> toRegister = new HashMap<>();
		toRegister.put("foo", "http://somewhere");
		fileList.register(toRegister);
		mappings = fileList.getMappings();
		Assert.assertEquals(2,  mappings.size());
		Assert.assertEquals("http://somewhere", mappings.get("wf:foo"));
		mappings = fileList.getMappings("wf:date1/*");
		Assert.assertEquals(1,  mappings.size());
		client.delete();
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
		client.delete();
	}
	
	@Test
	public void testConversionError()throws Exception{
		try{
			createWorkflow("src/test/resources/errors/wrong-activity-id.json");
		}catch(RESTException e) {
			assert 400 == e.getStatus();
			assert e.getErrorMessage().contains("no-such-activity");
		}
	}
	
	@Test
	public void testErrorMissingImport()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/errors/missing-import.json");
		waitWhileRunning(client);
		JSONObject wfProps = client.getProperties();
		JSONObject status = wfProps.getJSONObject("detailedStatus").
				getJSONObject("activities").
				getJSONArray("fail-on-missing-file").
				getJSONObject(0);
		Assert.assertTrue(status.getString("errorMessage").contains("SUBMIT_FAILED"));
		Assert.assertTrue(status.getString("status").equals("FAILED"));
	}
	
	@Test
	public void testErrorResubmit()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/errors/runtime-error.json");
		waitWhileRunning(client);
		JSONObject wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
		JSONObject status = wfProps.getJSONObject("detailedStatus").
				getJSONObject("activities").
				getJSONArray("fail-and-resubmit").
				getJSONObject(0);
		Assert.assertTrue(status.getString("errorMessage").contains("JOB_FAILED"));
		Assert.assertTrue(status.getString("status").equals("FAILED"));
		String url = client.getEndpoint().getUrl();
		String wfID = url.substring(url.lastIndexOf("/")+1);
		Statistics stats = getXNJS().get(InternalManager.class).getAction(wfID).getProcessingContext().get(Statistics.class);
		Assert.assertEquals(2, stats.getTotalJobs());
	}
	
	@Test
	public void testRunNoNotifications()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/no-notifications.json");
		JSONObject wfProps = client.getProperties();
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
	}

	private WorkflowFactoryClient getFactoryClient() {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/workflows";
		return new WorkflowFactoryClient(new Endpoint(url),kernel.getClientConfiguration(),null);
	}
	
	private WorkflowClient createWorkflow(JSONObject wf) throws Exception {
		wf.put("storageURL","https://somestorage");
		return getFactoryClient().submitWorkflow(wf);
	}

	private WorkflowClient createWorkflow(String wfFileName) throws Exception {
		JSONObject wf = wfFileName==null ? 
				new JSONObject() : 
				new JSONObject(FileUtils.readFileToString(new File(wfFileName), "UTF-8"));
		return createWorkflow(wf);
	}

	private JSONObject waitWhileRunning(WorkflowClient client) throws Exception {
		int c=0;
		do{
			Thread.sleep(1000);
		}while(c<60 && !client.isFinished());
		return client.getProperties();
	}
}
