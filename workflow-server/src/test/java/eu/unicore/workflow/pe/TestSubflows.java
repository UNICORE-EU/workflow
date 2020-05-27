package eu.unicore.workflow.pe;

import org.junit.Test;

import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.TestActivity;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;

public class TestSubflows extends TestBase {

	@Test
	public void testSubgroup()throws Exception{
		Validate.clear();
		String wfID="1";
		PEWorkflow wf=new PEWorkflow(wfID);
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup sub1=new ActivityGroup("sub1",wfID);
		TestActivity a2=new TestActivity("a2",wfID);
		sub1.setActivities(a2);
		
		wf.setActivities(a1,sub1);
		Transition t1=new Transition("a1->sub1",wfID,"a1","sub1");
		wf.setTransitions(t1);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		
	}

	@Test
	public void test2()throws Exception{
		Validate.clear();
		String wfID="2";
		PEWorkflow wf=new PEWorkflow(wfID);
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup sub1=new ActivityGroup("sub1",wfID);
		TestActivity a2=new TestActivity("a2",wfID);
		sub1.setActivities(a2);
		
		wf.setActivities(a1,sub1);
		Transition t1=new Transition("a1->sub1",wfID,"a1","sub1");
		wf.setTransitions(t1);
		
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		
		WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(wfID);
		assert wfc!=null;
		
		assert wfc.isSubFlow("sub1");
		
	}
}