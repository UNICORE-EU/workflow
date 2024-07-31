package eu.unicore.workflow.rest;

import java.io.File;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.workflow.WorkflowClient;
import eu.unicore.workflow.WorkflowFactoryClient;
import eu.unicore.workflow.pe.ContextFunctions;
import eu.unicore.workflow.pe.model.EvaluationException;

public class TestEvaluator extends WSSTestBase {
	
	@Test
	public void testEvaluator()throws Exception{
		WorkflowClient client = createWorkflow("src/test/resources/json/date1.json");
		waitWhileRunning(client);
		String url = client.getEndpoint().getUrl();
		String wfID = url.substring(url.lastIndexOf("/")+1);
		ContextFunctions ev = new ContextFunctions(wfID, "");
		assert ev.exitCodeEquals("date1", 0);
		assert ev.exitCodeNotEquals("date1", 123);
		assert ev.fileExists("date1", "stdout");
		assert ev.fileLengthGreaterThanZero("date1", "stdout");
		assert ev.fileExists("date1", "wf:date1/out");
		assert ev.fileLengthGreaterThanZero("date1", "wf:date1/out");
		assert 0 == ev.fileLength("date1", "stderr");
		assert 0 < ev.fileLength("date1", "wf:date1/out");
		ev = new ContextFunctions(wfID, null); // for coverage
		String s1 = ev.fileContent("date1", "stdout");
		String s2 = ev.fileContent("date1", "wf:date1/out");
		assert s1.equals(s2);
		assert !ev.fileExists("date1", "no-such-file");
		try {
			ev.fileLength("date1", "no-such-file");
			assert 1==0: "Expected exception";
		}catch(EvaluationException e) {
			System.out.println("As expected: "+e.getMessage());
		}
	}

	@Test
	public void testDateFormat() throws Exception {
		String test="2000-01-01 12:30";
		assert(new ContextFunctions("",null).after(test));
		assert(!new ContextFunctions("",null).before(test));
	}
	
	@Test
	public void testDateFormat2()throws Exception{
		Calendar c=Calendar.getInstance();
		ContextFunctions e=new ContextFunctions("",null);
		String test=e.getFormatted(c);
		System.out.println(test);
		Thread.sleep(2000);
		assert(e.after(test));
	}
	
	
	private WorkflowFactoryClient getFactoryClient() {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/workflows";
		return new WorkflowFactoryClient(new Endpoint(url),kernel.getClientConfiguration(),null);
	}
	
	private WorkflowClient createWorkflow(String wfFileName) throws Exception {
		JSONObject wf = wfFileName==null ? 
				new JSONObject() : 
				new JSONObject(FileUtils.readFileToString(new File(wfFileName), "UTF-8"));
		try{
			wf.put("name", wfFileName.substring(wfFileName.lastIndexOf("/")+1));
		}catch(Exception ex) {
			wf.put("name", "n/a");
		}
		wf.put("storageURL","https://somestorage");
		return getFactoryClient().submitWorkflow(wf);
	}

	protected JSONObject waitWhileRunning(WorkflowClient client) throws Exception {
		int c=0;;
		do{
			Thread.sleep(1000);
		}while(c<60 && !client.isFinished());
		return client.getProperties();
	}
}
