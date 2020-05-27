package eu.unicore.workflow.pe.files;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.junit.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.Job;
import eu.unicore.client.Job.Stage;
import eu.unicore.client.core.StorageClient;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.rest.WSSTestBase;

public class TestLocationHandling extends WSSTestBase {


	String storageURL = "http://localhost:64433/rest/core/storages/WORK";
	
	@Test
	public void testResolveSimple() throws Exception {
		String wfID = "1";
		String wfFile = "wf:date1/out";
		
		Map<String,String> locMap = new HashMap<>();
		locMap.put(wfFile, storageURL + "/WORK/files/1/date/out");
		Locations loc = new Locations();
		loc.setWorkflowID(wfID);
		loc.getLocations().putAll(locMap);
		PEConfig.getInstance().getLocationStore().write(loc);
		
		Job.Stage in1 = new Job.Stage();
		in1.from(wfFile).to("infile.dat");

		JSONArray in = new JSONArray();
		in.put(in1.getJSON());
		
		StagingPreprocessor sip = new StagingPreprocessor(wfID);
		JSONArray in_processed = sip.processImports(in);
		
		assert in_processed.getJSONObject(0).getString("From").equals(storageURL + "/WORK/files/1/date/out");
	}
	
	@Test
	public void testRegister1() throws Exception {
		String wfID = "2";
		Locations loc = new Locations();
		loc.setWorkflowID(wfID);
		PEConfig.getInstance().getLocationStore().write(loc);
		
		
		FileUtils.writeStringToFile(new File("target/data/WORK/test1.dat"), "test 1", "UTF-8");
		FileUtils.writeStringToFile(new File("target/data/WORK/test2.dat"), "test 2", "UTF-8");
		
		StorageClient sc = new StorageClient(new Endpoint(storageURL), 
				kernel.getClientConfiguration(),
				null);
		assert 2 == sc.getFiles("/").list(0, 10).size();
		
		JSONArray exports = new JSONArray();
		
		exports.put(new Stage().from("test1.dat").to("wf:out1").getJSON());
		exports.put(new Stage().from("test2.dat").to("wf:out2").getJSON());
		
		StageOutProcessor sop = new StageOutProcessor(wfID, storageURL, "n/a");
		sop.registerOutputs(exports);
		
		Map<String,String> map = PEConfig.getInstance().getLocationStore().read(wfID).getLocations();
		assert 2 == map.size();
		assert (storageURL+"/files/test1.dat").equals(map.get("wf:out1"));
		assert (storageURL+"/files/test2.dat").equals(map.get("wf:out2"));

	}
	
	
	@Test
	public void runWorkflow1() throws Exception {
		
	}
	
	
}
