package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.persist.Persist;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.RoutingActivity;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.pe.xnjs.HoldActivityProcessor;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.InternalManager;

public class TestWorkflowProcessing extends TestBase {

	@Test
	public void testTwoStep()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as = new ArrayList<>();
		as.add(new TestActivity("a1",wfID));
		as.add(new TestActivity("a2",wfID));
		Transition t=new Transition("a1->a2",wfID,"a1","a2");
		job.setActivities(as);
		job.setTransitions(t);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		//validations
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.before("a1", "a2"));
	}
	
	@Test
	public void testDiamond()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as = new ArrayList<>();
		as.add(new TestActivity("s",wfID));
		as.add(new TestActivity("a1",wfID));
		as.add(new TestActivity("a2",wfID));
		as.add(new TestActivity("a3",wfID));
		as.add(new TestActivity("e",wfID));
		Transition t=new Transition("s->a1",wfID,"s","a1");
		Transition t2=new Transition("s->a2",wfID,"s","a2");
		Transition t3=new Transition("a2->e",wfID,"a2","e");
		Transition t4=new Transition("a1->a3",wfID,"a1","a3");
		Transition t5=new Transition("a1->e",wfID,"a1","e");
		job.setActivities(as);
		job.setTransitions(t,t2,t3,t5,t4);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("s"));
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.wasInvoked("a3"));
		assert(Validate.wasInvoked("e"));
		assert(Validate.before("s", "a1"));
		assert(Validate.before("s", "a2"));
		assert(Validate.before("a1", "e"));
		assert(Validate.before("a1", "a3"));
		assert(Validate.before("a2", "e"));
		
		assert(Validate.getInvocations("e").intValue()==1);
	}

	@Test
	public void testHelperActivities()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<>();
		List<Transition>tr=new ArrayList<>();
		DeclareVariableActivity d1 = new DeclareVariableActivity("dec1", wfID, "b1", 
				VariableConstants.VARIABLE_TYPE_BOOLEAN, "true");
		DeclareVariableActivity d2 = new DeclareVariableActivity("dec2", wfID, "s1", 
				VariableConstants.VARIABLE_TYPE_STRING, "val");
		DeclareVariableActivity d3 = new DeclareVariableActivity("dec3", wfID, "f1", 
				VariableConstants.VARIABLE_TYPE_FLOAT, "3.1415");
		DeclareVariableActivity d4 = new DeclareVariableActivity("dec4", wfID, "i1", 
				VariableConstants.VARIABLE_TYPE_INTEGER, "42");
		job.setDeclarations(d1,d2,d3,d4);
		// routing
		as.add(new RoutingActivity("r1",wfID));
		as.add(new TestActivity("a1",wfID));
		as.add(new RoutingActivity("r2",wfID));
		as.add(new TestActivity("e1",wfID));
		
		tr.add(new Transition("r1->a1",wfID,"r1","a1"));
		tr.add(new Transition("a1->r2",wfID,"a1","r2"));
		tr.add(new Transition("r2->e1",wfID,"r2","e1"));
		
		// hold
		as.add(new HoldActivity("hold", wfID));
		as.add(new TestActivity("e2",wfID));
		tr.add(new Transition("e1->hold",wfID,"e1","hold"));
		tr.add(new Transition("hold->e2",wfID,"hold","e2"));
		
		// pause = hold with a non-zero sleep time
		HoldActivity pause = new HoldActivity("pause", wfID);
		pause.setSleepTime(1);
		as.add(pause);
		as.add(new TestActivity("end",wfID));
		tr.add(new Transition("e2->pause",wfID,"e2","pause"));
		tr.add(new Transition("pause->end",wfID,"pause","end"));
		
		
		job.setActivities(as);
		job.setTransitions(tr.toArray(new Transition[tr.size()]));
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		
		//wait for hold
		Persist<WorkflowContainer>persist=PEConfig.getInstance().getPersistence();
		int c=0;
		while(!persist.read(wfID).isHeld() && c<20){
			c++;
			Thread.sleep(1000);
		}
		assert HoldActivityProcessor.isHeld(wfID, "hold").getM1();
		
		//continue
		Map<String,String> params = new HashMap<>();
		params.put("b1", "false");
		params.put("s1", "bar");
		params.put("f1", "2.718");
		params.put("i1", "137");
		
		PEConfig.getInstance().getProcessEngine().resume(wfID, params);
		assert !HoldActivityProcessor.isHeld(wfID, "hold").getM1();
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("e1"));
		assert(Validate.wasInvoked("e2"));
		assert(Validate.wasInvoked("end"));
		
		assert(Validate.getInvocations("end").intValue()==1);
	}
	
	@Test
	public void testTwoStepWithCobrokering()throws Exception{
		Validate.clear();
		PEConfig.getInstance().setKeepAllActions(true);
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		job.setCoBrokerActivities(true);
		List<Activity>as=new ArrayList<Activity>();
		TestActivity a1=new TestActivity("a1",wfID);
		
		JSONObject jd1 = new JSONObject();
		jd1.put("ApplicationName","Date");
		jd1.put("Site name", "DEMO-SITE");
		a1.setJobDefinition(jd1);
		TestActivity a2=new TestActivity("a2",wfID);
	
		JSONObject jd2 = new JSONObject();
		jd2.put("ApplicationName","Python script");
		a2.setJobDefinition(jd2);
		as.add(a1);
		as.add(a2);
		Transition t=new Transition("a1->a2",wfID,"a1","a2");
		job.setActivities(as);
		job.setTransitions(t);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.before("a1", "a2"));
		
		int n=0;
		for(String id: Validate.actionIDs()){
			Action action=xnjs.get(InternalManager.class).getAction(id);
			Activity a=(Activity)action.getAjd();
			if(a instanceof TestActivity){
				JSONObject jdd=((TestActivity)a).getJobDefinition();
				assert jdd.optString("Site name",null) != null;
				n++;
			}
		}
		assert n==2;
		PEConfig.getInstance().setKeepAllActions(false);
	}

}