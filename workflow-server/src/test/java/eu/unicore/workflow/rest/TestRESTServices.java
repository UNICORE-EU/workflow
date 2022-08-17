package eu.unicore.workflow.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.iterators.FileIndirectionHelper;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.workflow.pe.iterators.WorkflowFileResolver;

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
		assert wfProps.getJSONArray("tags").length()==1;
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
		BaseServiceClient fileList = client.getFileList();
		fileList.setUpdateInterval(-1);
		JSONObject files = fileList.getProperties();
		System.out.println(files.toString(2));
		Assert.assertNotNull(files.getString("wf:date1/out"));
		// register
		JSONObject toRegister = new JSONObject();
		toRegister.put("foo", "http://somewhere");
		fileList.setProperties(toRegister);
		files = fileList.getProperties();
		Assert.assertEquals("http://somewhere", files.getString("wf:foo"));
		client.delete();
	}
	
	
	@Test
	public void testResolveWorkflowFiles()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/date1.json");
		waitWhileRunning(client);
		String wfURL = client.getEndpoint().getUrl();
		String wfID = wfURL.substring(wfURL.lastIndexOf("/")+1);
		WorkflowFileResolver resolver = new WorkflowFileResolver();
		FileSet fileset = new FileSet("wf:", new String[] {"*/*"}, null, false, false);
		Collection<Pair<String,Long>> results = resolver.resolve(wfID, fileset);
		System.out.println(results);
		Assert.assertEquals(2, results.size());
	}
	
	@Test
	public void testIndirectResolve()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/date1.json");
		waitWhileRunning(client);
		String wfURL = client.getEndpoint().getUrl();
		String wfID = wfURL.substring(wfURL.lastIndexOf("/")+1);

		String list = 
				"wf:date1/out\n" +
				"wf:date1/err\n";
		String smsURL = "http://localhost:64433/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(smsURL),kernel.getClientConfiguration(),null);
		HttpFileTransferClient ftc = (HttpFileTransferClient)sms.createImport("file_list.txt", false, -1, "BFT", null);
		ftc.writeAllData(new ByteArrayInputStream(list.getBytes()));
		ftc.delete();

		Collection<Pair<String,Long>>source = new ArrayList<>();
		source.add(new Pair<String,Long>(smsURL+"/files/file_list.txt",1l));
		FileIndirectionHelper fih = new FileIndirectionHelper(source, wfID);
		Collection<Pair<String,Long>> results = fih.resolve();
		System.out.println(results);
		Assert.assertEquals(2, results.size());
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
	public void testRunNoNotifications()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/no-notifications.json");
		JSONObject wfProps = client.getProperties();
		waitWhileRunning(client);
		wfProps = client.getProperties();
		System.out.println(wfProps.toString(2));
	}

	protected JSONObject waitWhileRunning(WorkflowClient client) throws Exception {
		int c=0;
		do{
			Thread.sleep(1000);
		}while(c<60 && !client.isFinished());
		return client.getProperties();
	}
}
