package eu.unicore.workflow.pe;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import eu.unicore.workflow.pe.iterators.Iteration;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.Condition;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.ScriptCondition;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.model.WhileGroup;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;

public class TestWhileLoopProcessing extends TestBase {

	@Test
	public void testWhileLoop()throws Exception{
		Validate.clear();
		PEConfig.getInstance().getPersistence().removeAll();
		
		String wfID=UUID.randomUUID().toString();
		PEWorkflow wf=new PEWorkflow(wfID);
		
		DeclareVariableActivity declare=new DeclareVariableActivity("decl",wfID,"C","INTEGER","0");
		ModifyVariableActivity modify=new ModifyVariableActivity("mod",wfID,"C","C++");
		
		int N=3;
		
		Iteration iter=new Iteration();
		String expr="C<"+N;
		iter.setIteratorName("C");
		Condition condition=new ScriptCondition("while_cond",wfID,expr);
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup whileBody=new ActivityGroup("while_body",wfID);
		whileBody.setLoopIteratorName("C");
		whileBody.setActivities(modify, a1);
		Transition tr=new Transition("mod->a1",wfID,"mod", "a1");
		whileBody.setTransitions(tr);
		whileBody.setIterate(iter);
		
		WhileGroup whileGroup=new WhileGroup("while_loop",wfID,whileBody,condition);
		
		wf.setActivities(whileGroup);
		wf.setDeclarations(declare);
			
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		
		waitForDone(wfID);
		
		assert Validate.wasInvoked("a1");
		assert Validate.getInvocations("a1")==N;

		//check stati
		List<PEStatus>s1=getStatus(wfID, "while_body");
		//must all be successful
		for(PEStatus st: s1){
			assert ActivityStatus.SUCCESS.equals(st.getActivityStatus());
		}
	}
	
	@Test
	public void testWhileLoopWithInternalCondition()throws Exception{
		Validate.clear();
		PEConfig.getInstance().getPersistence().removeAll();
		String wfID=UUID.randomUUID().toString();
		
		PEWorkflow wf=new PEWorkflow(wfID);
		
		DeclareVariableActivity declare=new DeclareVariableActivity("decl",wfID,"C","INTEGER","0");
		ModifyVariableActivity modify=new ModifyVariableActivity("mod",wfID,"C","C++");
		
		int N=3;
		
		Iteration iter=new Iteration();
		String expr="C<"+N;
		iter.setIteratorName("C");
		Condition condition=new ScriptCondition("while_cond",wfID,expr);
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup whileBody=new ActivityGroup("while_body",wfID);
		whileBody.setLoopIteratorName("C");
		Transition tr=new Transition("mod->a1",wfID,"mod", "a1");
		TestActivity a2=new TestActivity("a2",wfID);
		String innerScript="\"0\".equals(getIteration())";
		Condition cond=new ScriptCondition("inner_conditon",wfID,innerScript);
		Transition tr2=new Transition("a1->a2",wfID,"a1", "a2",cond);
		whileBody.setTransitions(tr,tr2);
		whileBody.setActivities(modify, a1, a2);
		whileBody.setIterate(iter);
		
		WhileGroup whileGroup=new WhileGroup("while_loop",wfID,whileBody,condition);
		
		wf.setActivities(whileGroup);
		wf.setDeclarations(declare);
			
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		waitForDone(wfID);
		
		assert Validate.wasInvoked("a1");
		assert Validate.getInvocations("a1")==N;

		//check stati
		List<PEStatus>s1=getStatus(wfID, "while_body");
		//must all be successful
		for(PEStatus st: s1){
			System.out.println(st);
			assert ActivityStatus.SUCCESS.equals(st.getActivityStatus());
		}
	}
	
	@Test
	public void testNestedWhileLoopWithInternalCondition()throws Exception{
		Validate.clear();
		PEConfig.getInstance().getPersistence().removeAll();
		
		String wfID=UUID.randomUUID().toString();
		PEWorkflow wf=new PEWorkflow(wfID);
		
		DeclareVariableActivity declare=new DeclareVariableActivity("decl",wfID,"C","INTEGER","0");
		ModifyVariableActivity modify=new ModifyVariableActivity("mod",wfID,"C","C++");
		
		int N=3;
		
		Iteration iter=new Iteration();
		String expr="C<"+N;
		iter.setIteratorName("C");
		Condition condition=new ScriptCondition("while_cond",wfID,expr);
		
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup whileBody=new ActivityGroup("while_body",wfID);
		whileBody.setLoopIteratorName("C");
		Transition tr=new Transition("mod->a1",wfID,"mod", "a1");
		TestActivity a2=new TestActivity("a2",wfID);
		String innerScript="\"0\".equals(getIteration())";
		Condition cond=new ScriptCondition("inner_conditon",wfID,innerScript);
		Transition tr2=new Transition("a1->a2",wfID,"a1", "a2",cond);
		whileBody.setTransitions(tr,tr2);
		whileBody.setActivities(modify, a1, a2);
		whileBody.setIterate(iter);
		
		WhileGroup whileGroup=new WhileGroup("while_loop",wfID,whileBody,condition);
		
		wf.setActivities(whileGroup);
		wf.setDeclarations(declare);
			
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		waitForDone(wfID);
		
		assert Validate.wasInvoked("a1");
		assert Validate.getInvocations("a1")==N;

		//check stati
		List<PEStatus>s1=getStatus(wfID, "while_body");
		//must all be successful
		for(PEStatus st: s1){
			assert ActivityStatus.SUCCESS.equals(st.getActivityStatus());
		}
	}
	
	@Test
	public void testWhileLoopFaultyCondition()throws Exception{
		Validate.clear();
		PEConfig.getInstance().getPersistence().removeAll();
		
		String wfID=UUID.randomUUID().toString();
		PEWorkflow wf=new PEWorkflow(wfID);
		
		DeclareVariableActivity declare=new DeclareVariableActivity("decl",wfID,"C","INTEGER","0");
		ModifyVariableActivity modify=new ModifyVariableActivity("mod",wfID,"C","C++");
		
		Iteration iter=new Iteration();
		// this is a faulty expression
		String expr="XXXX<11";
		iter.setIteratorName("C");
		Condition condition=new ScriptCondition("while_cond",wfID,expr);
		
		TestActivity a1=new TestActivity("a1",wfID);
		ActivityGroup whileBody=new ActivityGroup("while_body",wfID);
		whileBody.setLoopIteratorName("C");
		whileBody.setActivities(modify, a1);
		Transition tr=new Transition("mod->a1",wfID,"mod", "a1");
		whileBody.setTransitions(tr);
		whileBody.setIterate(iter);
		
		WhileGroup whileGroup=new WhileGroup("while_loop",wfID,whileBody,condition);
		
		wf.setActivities(whileGroup);
		wf.setDeclarations(declare);
			
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		waitForDone(wfID);
		
		assert !Validate.wasInvoked("a1");
		
		// must have an entry containing a proper error message
		List<PEStatus>s1=getStatus(wfID, "while_loop");
		assert s1.size() == 1;
		assert s1.get(0).toString().contains("XXXX");
	}
	
}