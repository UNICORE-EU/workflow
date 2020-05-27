package eu.unicore.workflow.pe.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.client.Job;
import eu.unicore.workflow.pe.iterators.ChunkedFileIterator;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class TestInsertVariablesFilter {


    @Test
	public void testInsertVariables() throws JSONException {
		ProcessVariables vars=new ProcessVariables();
		vars.put("ForEachIterator", "foo");
		vars.put("SOME_VAR", "bar");
		InsertVariablesFilter filter = new InsertVariablesFilter(vars);

		Job builder=new Job();		
		builder.application("Date");
		builder.stagein().from("${ForEachIterator}").to("infile");
		builder.environment("x","${SOME_VAR}");
		JSONObject doc = builder.getJSON();

		JSONObject filtered = filter.filter(doc);
		
		assert 1==filtered.getJSONArray("Imports").length();
		JSONObject in1 = filtered.getJSONArray("Imports").getJSONObject(0);
		assert in1.getString("From").equals("foo");
		
		JSONArray env = filtered.getJSONArray("Environment");
		assert 1==env.length();
		System.out.println(env);
		assert "x=bar".equals(env.getString(0));
	}
	

    @Test
	public void testManipulateStageInDefinitionsInChunkedLoop() throws JSONException {
		ProcessVariables vars=new ProcessVariables();
		vars.put(ChunkedFileIterator.PV_IS_CHUNKED, Boolean.TRUE);
		vars.put(ChunkedFileIterator.PV_ITERATOR_NAME, "ForEachIterator");
		vars.put(ChunkedFileIterator.PV_THIS_CHUNK_SIZE, Integer.valueOf(2));
		vars.put(ChunkedFileIterator.PV_FILENAME+"_1", "foo_1");
		vars.put(ChunkedFileIterator.PV_FILENAME+"_2", "foo_2");
		vars.put(ChunkedFileIterator.PV_FILENAME_FORMAT, ChunkedFileIterator.DEFAULT_FORMAT);

		InsertVariablesFilter filter = new InsertVariablesFilter(vars);


		Job builder=new Job();		
		builder.application("Date");
		builder.stagein().from("${ForEachIterator_VALUE}").to("infile");
		JSONObject doc = builder.getJSON();
		JSONObject filtered = filter.filter(doc);
		
		JSONArray in = filtered.getJSONArray("Imports");
		assert 2==in.length();
		assert in.getJSONObject(0).getString("From").equals("foo_1");
		assert in.getJSONObject(0).getString("To").equals("1_infile");

		assert in.getJSONObject(1).getString("From").equals("foo_2");
		assert in.getJSONObject(1).getString("To").equals("2_infile");

	}


    @Test
	public void testManipulateStageInDefinitionsUsingCustomPattern() throws JSONException {
		ProcessVariables vars=new ProcessVariables();
		vars.put(ChunkedFileIterator.PV_IS_CHUNKED, Boolean.TRUE);
		vars.put(ChunkedFileIterator.PV_ITERATOR_NAME, "ForEachIterator");
		vars.put(ChunkedFileIterator.PV_THIS_CHUNK_SIZE, Integer.valueOf(2));
		vars.put(ChunkedFileIterator.PV_FILENAME+"_1", "foo_1");
		vars.put(ChunkedFileIterator.PV_FILENAME+"_2", "foo_2");
		vars.put(ChunkedFileIterator.PV_FILENAME_FORMAT, "{1}_{0,number,0000}{2}");


		InsertVariablesFilter filter = new InsertVariablesFilter(vars);


		Job builder=new Job();		
		builder.application("Date");
		builder.stagein().from("${ForEachIterator_VALUE}").to("infile.txt");
		JSONObject doc = builder.getJSON();
		JSONObject filtered = filter.filter(doc);
		
		JSONArray in = filtered.getJSONArray("Imports");
		assert 2==in.length();
		
		assert in.getJSONObject(0).getString("From").equals("foo_1");
		assert in.getJSONObject(0).getString("To").equals("infile_0001.txt");

		assert in.getJSONObject(1).getString("From").equals("foo_2");
		assert in.getJSONObject(1).getString("To").equals("infile_0002.txt");

	}
	

    @Test
	public void testManipulateStageInDefinitionsUsingSimplePattern() throws JSONException {
		ProcessVariables vars=new ProcessVariables();
		vars.put(ChunkedFileIterator.PV_IS_CHUNKED, Boolean.TRUE);
		vars.put(ChunkedFileIterator.PV_ITERATOR_NAME, "ForEachIterator");
		vars.put(ChunkedFileIterator.PV_THIS_CHUNK_SIZE, Integer.valueOf(2));
		vars.put(ChunkedFileIterator.PV_FILENAME+"_1", "foo_1");
		vars.put(ChunkedFileIterator.PV_FILENAME+"_2", "foo_2");

		InsertVariablesFilter filter = new InsertVariablesFilter(vars);

		Job builder=new Job();		
		builder.application("Date");
		builder.stagein().from("${ForEachIterator_VALUE}").to("infile_*.txt");
		JSONObject doc = builder.getJSON();
		JSONObject filtered = filter.filter(doc);
		JSONArray in = filtered.getJSONArray("Imports");
		assert 2==in.length();
		
		assert in.getJSONObject(0).getString("From").equals("foo_1");
		assert in.getJSONObject(0).getString("To").equals("infile_0001.txt");

		assert in.getJSONObject(1).getString("From").equals("foo_2");
		assert in.getJSONObject(1).getString("To").equals("infile_0002.txt");

	}

}
