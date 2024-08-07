package eu.unicore.workflow.pe;

import java.io.File;
import java.util.UUID;

import org.chemomentum.dsws.ConversionResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.workflow.json.Converter;
import eu.unicore.workflow.pe.iterators.VariableSetIterator;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ForGroup;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.xnjs.TestBase;
import eu.unicore.xnjs.util.IOUtils;

/**
 * runs example .json workflows
 */
public class TestExamples  extends TestBase {

	@Test
	public void testDiamond() throws Exception {
		String file="src/test/resources/json/diamond.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
		PEWorkflow wf = res.getConvertedWorkflow();
		doProcess(wf);
	}

	@Test
	public void testIfElse() throws Exception {
		String file="src/test/resources/json/ifelse.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
		PEWorkflow wf = res.getConvertedWorkflow();
		doProcess(wf);
	}


	@Test
	public void testWhile() throws Exception {
		String file="src/test/resources/json/while.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
		PEWorkflow wf = res.getConvertedWorkflow();
		doProcess(wf);
	}

	
	@Test
	public void testForeachValues() throws Exception {
		String file="src/test/resources/json/foreach.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
		PEWorkflow wf = res.getConvertedWorkflow();
		doProcess(wf);
	}
	
	@Test
	public void testForeachVariables() throws Exception {
		String file="src/test/resources/json/foreach_varset.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
		PEWorkflow wf = res.getConvertedWorkflow();
		ActivityGroup ag=res.getConvertedWorkflow();
		assert ag!=null;
		Activity a1=ag.getActivity("for1");
		assert a1 instanceof ForGroup;
		ForGroup fg = (ForGroup)a1;
		VariableSetIterator vsi = (VariableSetIterator)fg.getBody().getIterate();
		assert vsi.getVariableSets().length==2;
		
		doProcess(wf);
		try (WorkflowContainer wfc = PEConfig.getInstance().getPersistence().getForUpdate(wfID)){
			wfc.compact(0);
			assert wfc.getSize()==0;
		}
	}
	
	@Test
	public void testForeachFileset() throws Exception {
		String file="src/test/resources/json/foreach_fileset.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		assert !res.hasConversionErrors(): String.valueOf(res.getConversionErrors());
	//	PEWorkflow wf = res.getConvertedWorkflow();
	//	doProcess(wf); // TBD
	}
}
