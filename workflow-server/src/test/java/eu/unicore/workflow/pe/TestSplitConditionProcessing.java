package eu.unicore.workflow.pe;

import java.util.UUID;

import org.junit.Test;

import eu.unicore.workflow.pe.model.Activity.MergeType;
import eu.unicore.workflow.pe.model.Activity.SplitType;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.ScriptCondition;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;

public class TestSplitConditionProcessing extends TestBase {

	@Test
	public void testFollowFirstMatchingConditionOnly()throws Exception{
		Validate.clear();
		
		String wfID=UUID.randomUUID().toString();
		TestActivity a1=new TestActivity("a1",wfID);
		a1.setSplitType(SplitType.FOLLOW_FIRST_MATCHING);
		TestActivity a2=new TestActivity("a2",wfID);
		TestActivity a3=new TestActivity("a3",wfID);
		PEWorkflow wf=new PEWorkflow(wfID);
		wf.setActivities(a1,a2,a3);
		Condition cond=new ScriptCondition("cond1",wfID,"true;");
		Transition t1=new Transition("a1->a2",wfID,"a1","a2",cond);
		Transition t2=new Transition("a1->a3",wfID,"a1","a3");
		wf.setTransitions(t1,t2);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(!Validate.wasInvoked("a3"));
	}

	@Test
	public void testDiamondShapeWithOneBranchFollowed()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		TestActivity a1=new TestActivity("a1",wfID);
		a1.setSplitType(SplitType.FOLLOW_FIRST_MATCHING);
		TestActivity a2=new TestActivity("a2",wfID);
		TestActivity a3=new TestActivity("a3",wfID);
		TestActivity a4=new TestActivity("a4",wfID);
		a4.setMergeType(MergeType.MERGE);
		PEWorkflow wf=new PEWorkflow(wfID);
		wf.setActivities(a1,a2,a3,a4);
		Condition cond=new ScriptCondition("cond1",wfID,"true;");
		Transition t1=new Transition("a1->a2",wfID,"a1","a2",cond);
		Transition t2=new Transition("a1->a3",wfID,"a1","a3");
		Transition t3=new Transition("a2->a4",wfID,"a2","a4");
		Transition t4=new Transition("a3->a4",wfID,"a3","a4");
		wf.setTransitions(t1,t2,t3,t4);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert Validate.wasInvoked("a1");
		assert Validate.wasInvoked("a2");
		assert !Validate.wasInvoked("a3");
		assert Validate.wasInvoked("a4");
		assert Validate.before("a1", "a4");
		assert 1==Validate.getInvocations("a4");
	}

	@Test
	public void testDiamondShapeWithBypass()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		TestActivity a0=new TestActivity("a0",wfID);
		TestActivity a1=new TestActivity("a1",wfID);
		a1.setSplitType(SplitType.FOLLOW_FIRST_MATCHING);
		TestActivity a2=new TestActivity("a2",wfID);
		TestActivity a3=new TestActivity("a3",wfID);
		TestActivity a4=new TestActivity("a4",wfID);
		a4.setMergeType(MergeType.MERGE);
		PEWorkflow wf=new PEWorkflow(wfID);
		wf.setActivities(a0,a1,a2,a3,a4);
		Condition cond=new ScriptCondition("cond1",wfID,"true;");
		Transition t1=new Transition("a1->a2",wfID,"a1","a2",cond);
		Transition t2=new Transition("a1->a3",wfID,"a1","a3");
		Transition t3=new Transition("a2->a4",wfID,"a2","a4");
		Transition t4=new Transition("a3->a4",wfID,"a3","a4");
		Transition t5=new Transition("a0->a4",wfID,"a0","a4");
		wf.setTransitions(t1,t2,t3,t4,t5);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);

		waitForDone(wfID);
		
		assert Validate.wasInvoked("a0");
		assert Validate.wasInvoked("a1");
		assert Validate.wasInvoked("a2");
		assert !Validate.wasInvoked("a3");
		assert Validate.wasInvoked("a4");
	}
}