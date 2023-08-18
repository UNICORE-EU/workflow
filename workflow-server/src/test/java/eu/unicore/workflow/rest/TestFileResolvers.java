package eu.unicore.workflow.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;
import eu.unicore.workflow.pe.iterators.FileIndirectionHelper;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.workflow.pe.iterators.StorageResolver;
import eu.unicore.workflow.pe.iterators.WorkflowFileResolver;

public class TestFileResolvers extends WSSTestBase {

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
		
		StorageResolver smsResolver = new StorageResolver();
		EnumerationClient jobList = client.getJobList();
		String jobURL = jobList.getUrls(0, 10).get(0);
		JobClient jc = jobList.createClient(jobURL, JobClient.class);
		String smsURL = jc.getLinkUrl("workingDirectory");
		fileset = new FileSet(smsURL, new String[] {"std*"}, null, false, false);
		results = smsResolver.resolve(wfID, fileset);
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
		source.add(new Pair<>(smsURL+"/files/file_list.txt",1l));
		FileIndirectionHelper fih = new FileIndirectionHelper(source, wfID);
		Collection<Pair<String,Long>> results = fih.resolve();
		System.out.println(results);
		Assert.assertEquals(2, results.size());
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
