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
		locMap.put(wfFile, storageURL + "/files/1/date/out");
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
		
		assert in_processed.getJSONObject(0).getString("From").equals(storageURL + "/files/1/date/out");
	}
	
	@Test
	public void testRegister1() throws Exception {
		String wfID = "2";
		Locations loc = new Locations();
		loc.setWorkflowID(wfID);
		PEConfig.getInstance().getLocationStore().write(loc);
		
		FileUtils.deleteQuietly(new File("target/data/WORK"));
		
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
	public void testRegisterWildcards() throws Exception {
		String wfID = "3";
		Locations loc = new Locations();
		loc.setWorkflowID(wfID);
		PEConfig.getInstance().getLocationStore().write(loc);
		
		FileUtils.deleteQuietly(new File("target/data/WORK"));
		for(int i = 0 ; i<5; i++) {
			FileUtils.writeStringToFile(new File("target/data/WORK/test"+i+".dat"), "test "+i, "UTF-8");
			FileUtils.writeStringToFile(new File("target/data/WORK/subdir/sub"+i+".dat"), "sub "+i, "UTF-8");
		}
		
		JSONArray exports = new JSONArray();
		exports.put(new Stage().from("test*.dat").to("wf:out1").getJSON());
		exports.put(new Stage().from("subdir/sub*.dat").to("wf:out1/sub/").getJSON());
		
		StageOutProcessor sop = new StageOutProcessor(wfID, storageURL, "n/a");
		sop.registerOutputs(exports);
	
		Map<String,String> map = PEConfig.getInstance().getLocationStore().read(wfID).getLocations();
		for(Map.Entry<String,String>e: map.entrySet()) {
			System.out.println(e.getKey()+" -> "+e.getValue());
		}
		assert 10 == map.size();
		assert (storageURL+"/files/test0.dat").equals(map.get("wf:out1/test0.dat"));
		assert (storageURL+"/files/test4.dat").equals(map.get("wf:out1/test4.dat"));
		assert (storageURL+"/files/subdir/sub0.dat").equals(map.get("wf:out1/sub/sub0.dat"));
		assert (storageURL+"/files/subdir/sub4.dat").equals(map.get("wf:out1/sub/sub4.dat"));
	}
	
	@Test
	public void testResolveWildcard() throws Exception {
		String wfID = "4";
		String wfFile = "wf:out1/";
		
		Map<String,String> locMap = new HashMap<>();
		for(int i=0; i<5; i++) {
			locMap.put(wfFile+"file"+i, storageURL + "/files/out"+i);
		}
		
		Locations loc = new Locations();
		loc.setWorkflowID(wfID);
		loc.getLocations().putAll(locMap);
		PEConfig.getInstance().getLocationStore().write(loc);
		
		Job.Stage in1 = new Job.Stage();
		in1.from(wfFile+"*").to("/");

		JSONArray in = new JSONArray();
		in.put(in1.getJSON());
		
		StagingPreprocessor sip = new StagingPreprocessor(wfID);
		JSONArray in_processed = sip.processImports(in);
		System.out.println(in_processed);
		assert in_processed.length()==5;
		assert in_processed.getJSONObject(0).getString("From").contains(storageURL + "/files/out");
	}
}
