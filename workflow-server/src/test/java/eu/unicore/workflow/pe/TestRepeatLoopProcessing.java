package eu.unicore.workflow.pe;

import java.util.List;

import org.junit.Test;

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