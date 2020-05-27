package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.persist.Persist;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.InternalManager;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.PauseActivity;
import eu.unicore.workflow.pe.model.RoutingActivity;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.HoldActivityProcessor;
import eu.unicore.workflow.pe.xnjs.TestActivity;
import eu.unicore.workflow.pe.xnjs.Validate;
import eu.unicore.workflow.xnjs.TestBase;

public class TestWorkflowProcessing extends TestBase {

	@Test
	public void testTwoStep()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
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

	/*
	 * builds and runs a diamond shaped graph, where one branch takes longer than the other.
	 */
	@Test
	public void testDiamondGraph()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("s",wfID));
		as.add(new TestActivity("a1",wfID,50));
		as.add(new TestActivity("a2",wfID,500));
		as.add(new TestActivity("e",wfID));
		Transition t=new Transition("s->a1",wfID,"s","a1");
		Transition t2=new Transition("s->a2",wfID,"s","a2");
		Transition t3=new Transition("a2->e",wfID,"a2","e");
		Transition t4=new Transition("a1->e",wfID,"a1","e");
		job.setActivities(as);
		job.setTransitions(t,t2,t3,t4);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("s"));
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.wasInvoked("e"));
		assert(Validate.before("s", "a1"));
		assert(Validate.before("s", "a2"));
		assert(Validate.before("a1", "e"));
		assert(Validate.before("a2", "e"));
		//check that "e" was executed once only
		assert(Validate.getInvocations("e").intValue()==1);
	}
	
	/*
	 * builds and runs a diamond shaped graph, where one branch takes longer than the other.
	 */
	@Test
	public void testDiamondWithTwoActivitiesOnOneBranch()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("s",wfID));
		as.add(new TestActivity("a1",wfID,50));
		as.add(new TestActivity("a2",wfID,500));
		as.add(new TestActivity("a2b",wfID,50));
		as.add(new TestActivity("e",wfID));
		Transition t=new Transition("s->a1",wfID,"s","a1");
		Transition t2=new Transition("s->a2",wfID,"s","a2");
		Transition t3=new Transition("a2->a2b",wfID,"a2","a2b");
		Transition t3b=new Transition("a2b->e",wfID,"a2b","e");
		Transition t4=new Transition("a1->e",wfID,"a1","e");
		job.setActivities(as);
		job.setTransitions(t,t2,t3,t3b,t4);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("s"));
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.wasInvoked("a2b"));
		assert(Validate.wasInvoked("e"));
		
		assert(Validate.before("s", "a1"));
		assert(Validate.before("s", "a2"));
		assert(Validate.before("s", "a2b"));
		assert(Validate.before("a1", "e"));
		assert(Validate.before("a2", "a2b"));
		assert(Validate.before("a2", "e"));
		assert(Validate.before("a2b", "e"));
		
		assert(Validate.getInvocations("e").intValue()==1);
	}
	
	/*
	 * builds and runs a diamond shaped graph, with an additional side branch
	 */
	@Test
	public void testDiamondWithSideBranch()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("s",wfID));
		as.add(new TestActivity("a1",wfID,50));
		as.add(new TestActivity("a2",wfID,500));
		as.add(new TestActivity("a3",wfID,50));
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
		assert(Validate.before("a2", "e"));
		
		assert(Validate.getInvocations("e").intValue()==1);
	}

	@Test
	public void testRouting()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new RoutingActivity("r1",wfID));
		as.add(new TestActivity("a1",wfID,50));
		as.add(new RoutingActivity("r2",wfID));
		as.add(new TestActivity("e",wfID));
		Transition t=new Transition("r1->a1",wfID,"r1","a1");
		Transition t3=new Transition("a1->r2",wfID,"a1","r2");
		Transition t4=new Transition("r2->e",wfID,"r2","e");
		job.setActivities(as);
		job.setTransitions(t,t3,t4);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("e"));
		assert(Validate.before("a1", "e"));
		assert(Validate.getInvocations("e").intValue()==1);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testHoldTask()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("a1",wfID));
		as.add(new HoldActivity("hold", wfID));
		as.add(new TestActivity("a2",wfID));
		Transition t1=new Transition("a1->hold",wfID,"a1","hold");
		Transition t2=new Transition("hold->a2",wfID,"hold","a2");
		job.setActivities(as);
		job.setTransitions(t1,t2);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		Persist<WorkflowContainer>persist=PEConfig.getInstance().getPersistence();
		
		//wait for hold
		int c=0;
		while(!persist.read(wfID).isHeld() && c<10){
			c++;
			Thread.sleep(1000);
		}
		assert HoldActivityProcessor.isHeld(wfID, "hold").getM1();
		
		//continue
		WorkflowContainer wfc=persist.getForUpdate(wfID);
		wfc.resume(Collections.EMPTY_MAP);
		persist.write(wfc);
		assert !HoldActivityProcessor.isHeld(wfID, "hold").getM1();
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.before("a1", "a2"));
	}
	
	@Test
	public void testPauseTask()throws Exception{
		Validate.clear();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("a1",wfID));
		PauseActivity pause = new PauseActivity("pause", wfID);
		pause.setSleepTime(5);
		as.add(pause);
		as.add(new TestActivity("a2",wfID));
		Transition t1=new Transition("a1->pause",wfID,"a1","pause");
		Transition t2=new Transition("pause->a2",wfID,"pause","a2");
		job.setActivities(as);
		job.setTransitions(t1,t2);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		
		waitForDone(wfID);
		
		assert(Validate.wasInvoked("a1"));
		assert(Validate.wasInvoked("a2"));
		assert(Validate.before("a1", "a2"));
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