package eu.unicore.workflow.pe;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.chemomentum.dsws.ConversionResult;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.workflow.json.Converter;
import eu.unicore.workflow.pe.iterators.Iteration;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.RepeatGroup;
import eu.unicore.workflow.pe.model.ScriptCondition;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;

public class TestRepeatLoopProcessing extends TestBase {
	
	@Test
	public void testConvertedRepeatWF() throws Exception {
		String file="src/test/resources/json/repeat.json";
		String wfID=UUID.randomUUID().toString();
		JSONObject json = new JSONObject(IOUtils.readFile(new File(file)));
		ConversionResult res = new Converter(true).convert(wfID, json);
		printErrors(res);
		assert !res.hasConversionErrors(): res.getConversionErrors();
		
		PEWorkflow wf = res.getConvertedWorkflow();
		doProcess(wf);
	}
	
	@Test
	@Ignore
	public void testRepeatLoop()throws Exception{
		Validate.clear();
		PEConfig.getInstance().getPersistence().removeAll();
		
		String wfID="wf";
		
		PEWorkflow wf=new PEWorkflow(wfID);
		
		DeclareVariableActivity declare=new DeclareVariableActivity("decl",wfID,"C","INTEGER","0");
		
		ModifyVariableActivity modify=new ModifyVariableActivity("mod",wfID,"C","C++;");
		
		Iteration iter=new Iteration();
		int N=2;
		
		String expr="C=="+N+";";
		iter.setIteratorName("C");
		Condition condition=new ScriptCondition("repeat_cond",wfID,expr);
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup repeatBody=new ActivityGroup("repeat_body",wfID);
		repeatBody.setLoopIteratorName("C");
		repeatBody.setActivities(modify, a1);
		Transition tr=new Transition("mod->a1",wfID,"mod", "a1");
		repeatBody.setTransitions(tr);
		repeatBody.setIterate(iter);
		
		RepeatGroup repeatGroup=new RepeatGroup("repeat_loop",wfID,repeatBody,condition);
		
		wf.setActivities(repeatGroup);
		wf.setDeclarations(declare);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert Validate.wasInvoked("a1");
		assert Validate.getInvocations("a1")==N;
		
		//check stati
		List<PEStatus>s1=getStatus("wf", "repeat_body");
		//must all be successful
		for(PEStatus s: s1){
			assert ActivityStatus.SUCCESS.equals(s.getActivityStatus());
		}
		
	}


}