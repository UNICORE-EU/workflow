package eu.unicore.workflow.json;

import java.io.File;
import java.util.Arrays;

import org.chemomentum.dsws.ConversionResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.workflow.pe.iterators.ForEachFileIterator;
import eu.unicore.workflow.pe.iterators.Iteration;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ForGroup;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.ScriptCondition;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.model.WhileGroup;

public class TestConversion {

	@Test
	public void testConvertSimpleDate()throws Exception{
		String file="src/test/resources/json/date1.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		PEWorkflow ag=res.getConvertedWorkflow();
		assert ag instanceof PEWorkflow;
		assert ag!=null;
		Activity act=ag.getActivity("date1");
		assert act!=null;
		JSONExecutionActivity jsdl=(JSONExecutionActivity)act;
		
		JSONObject job=jsdl.getJobDefinition();
		assert job!=null;
		assert job.toString().contains("Date");
		assert jsdl.isIgnoreFailure();
		assert "true".equals(jsdl.getOption(JSONExecutionActivity.OPTION_IGNORE_FAILURE));
		assert "10".equals(jsdl.getOption(JSONExecutionActivity.OPTION_MAX_RESUBMITS));
	}

	@Test
	public void testConvertTwoDate()throws Exception{
		String file="src/test/resources/json/twostep.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		ActivityGroup ag=res.getConvertedWorkflow();
		assert ag!=null;
		Activity act=ag.getActivity("date1");
		assert act!=null;
		JSONExecutionActivity jsdl=(JSONExecutionActivity)act;
		JSONObject job = jsdl.getJobDefinition();
		assert job!=null;
		assert job.toString().contains("Date");
		
		assert ag.getTransitions()!=null;
		assert ag.getTransitions().size()==1;
		Transition tr1=ag.getTransitions().get(0);
		assert tr1!=null;
	}
	
	@Test
	public void testConvertForEach()throws Exception{
		String file="src/test/resources/json/foreach.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		ActivityGroup ag=res.getConvertedWorkflow();
		assert ag!=null;
		
		Activity a1=ag.getActivity("for1");
		assert a1 instanceof ForGroup;
		
		ForGroup fg=(ForGroup)a1;
		assert fg.getMaxConcurrentActivities()==17;
		
		Activity body=fg.getBody();
		assert body instanceof ActivityGroup;
		
		assert ((ActivityGroup)body).getActivities().size()==1; 
		
		assert "IT".equals(((Iteration)body.getIterate()).getIteratorName());
	}
	
	
	@Test
	public void testConvertForEachFilesetChunked()throws Exception{
		String file="src/test/resources/json/foreach_fileset_chunked.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		ActivityGroup ag=res.getConvertedWorkflow();
		ForGroup fg=(ForGroup)ag.getActivity("for1");
		Activity body=fg.getBody();
		assert ((ActivityGroup)body).getActivities().size()==1;
		ForEachFileIterator cfi = (ForEachFileIterator)body.getIterate();
		assert 2==cfi.getChunkSize();
		assert "file_{0}.{2}".equals(cfi.getFormatString());
	}

	@Test
	public void testConvertStaging()throws Exception{
		String file="src/test/resources/json/two-with-outputs.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		PEWorkflow ag=res.getConvertedWorkflow();
		
		Activity act=ag.getActivity("date1");
		assert act!=null;
		JSONExecutionActivity jsdl=(JSONExecutionActivity)act;
		JSONObject job=jsdl.getJobDefinition();
		assert job!=null;
		JSONArray exp = job.optJSONArray("Exports");
		assert exp!=null;
		assert exp.length() == 1;
	}
	
	@Test
	public void testSubflow1() throws Exception {
		String file="src/test/resources/json/subflow1.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		PEWorkflow ag=res.getConvertedWorkflow();
		Activity act=ag.getActivity("date1");
		assert act!=null;
		ActivityGroup subGroup = (ActivityGroup)ag.getActivity("sw1");
		assert subGroup!=null;
		Activity act1 = subGroup.getActivity("sw1-date1");
		assert act1!=null;
		
	}
	
	@Test
	public void testSubflow2() throws Exception {
		String file="src/test/resources/json/subflow2.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		assert wfID.equals(res.getWorkflowID());
		PEWorkflow ag=res.getConvertedWorkflow();
		Activity act=ag.getActivity("date1");
		assert act!=null;
		ActivityGroup subGroup = (ActivityGroup)ag.getActivity("sw1");
		assert subGroup!=null;
		Activity act1 = subGroup.getActivity("sw1-date1");
		assert act1!=null;
		
	}
	

	@Test
	public void testWhile() throws Exception {
		String file="src/test/resources/json/while.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		PEWorkflow ag=res.getConvertedWorkflow();
		WhileGroup whileGrp = (WhileGroup)ag.getActivity("while");
		ScriptCondition sc = (ScriptCondition) whileGrp.getCondition();
		assert "C<=2".equals(sc.getScript()): sc.getScript();
	}
	
	@Test
	public void testDiamond() throws Exception {
		String file="src/test/resources/json/diamond2.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		PEWorkflow ag=res.getConvertedWorkflow();
		assert ag.getActivities().size()==6;
		assert ag.getActivity("date3")!=null;
	}
	
	@Test
	public void testHold() throws Exception {
		String file="src/test/resources/json/hold-with-variables.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		PEWorkflow ag=res.getConvertedWorkflow();
		assert ag.getActivities().size()==3;
		HoldActivity hold = (HoldActivity)ag.getActivity("hold1");
		assert 1800==hold.getSleepTime();
	}
	
	@Test
	public void testConvertVariables() throws Exception {
		String file="src/test/resources/json/variables.json";
		String wfID="1";
		JSONObject wf = new JSONObject(IOUtils.readFile(new File(file)));
		assert wf!=null;
		ConversionResult res = new Converter().convert(wfID, wf);
		assert res!=null;
		printErrors(res);
		assert !res.hasConversionErrors();
		PEWorkflow ag=res.getConvertedWorkflow();
		assert ag.getActivities().size()==1;
		assert ag.getDeclarations().size()==2;
		for (DeclareVariableActivity a: ag.getDeclarations()) {
			assert Arrays.asList("FOO","COUNTER").contains(a.getVariableName());
		}
	}
	
	protected void printErrors(ConversionResult res){
		if(res.hasConversionErrors()){
			System.out.println("ERRORS:");
			for(String err:res.getConversionErrors()){
				System.out.println(err+"\n");
			}
		}
	}
}
