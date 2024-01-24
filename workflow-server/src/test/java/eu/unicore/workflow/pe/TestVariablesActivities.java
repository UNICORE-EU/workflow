package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;
import eu.unicore.workflow.xnjs.TestBase;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;

public class TestVariablesActivities extends TestBase {

	@Test
	public void test1()throws Exception{
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		List<Activity>as = new ArrayList<>();
		as.add(new DeclareVariableActivity("a1", wfID, "test1", "INTEGER","1"));
		as.add(new DeclareVariableActivity("a2", wfID, "test2", "INTEGER","1"));
		job.setActivities(as);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
	
		waitForDone(wfID);
	}
	
	@Test
	public void testDeclareVariable()throws Exception{
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=new PEWorkflow(wfID);
		int initialValue=1;
		double initialDoubleValue=3.14;
		
		List<Activity>as=new ArrayList<Activity>();
		
		as.add(new DeclareVariableActivity("a1",wfID,"test1","INTEGER",String.valueOf(initialValue)));
		as.add(new DeclareVariableActivity("a2",wfID,"test2","FLOAT",String.valueOf(initialDoubleValue)));
		as.add(new DeclareVariableActivity("a3",wfID,"test3","STRING","TEST"));
		
		job.setActivities(as);
		job.init();
		PEConfig.getInstance().getProcessEngine().process(job, null);
		waitForDone(wfID);
		
		//check that we have the corresponding variables in the PC with the requested values
		Action action=xnjs.get(InternalManager.class).getAction(wfID);
		assert(action!=null);
		ProcessVariables pv=action.getProcessingContext().get(ProcessVariables.class);
		assert(pv!=null);
		
		Integer test1=pv.get("test1", Integer.class);
		assert(test1!=null);
		assert(test1.intValue()==initialValue);
		
		Double test2=pv.get("test2", Double.class);
		assert(test2!=null);
		assert(test2.doubleValue()==initialDoubleValue);
		
		String test3=pv.get("test3", String.class);
		assert(test3!=null);
		assert("TEST".equals(test3));
		
		
	}
	
	@Test
	public void testModifyVariable()throws Exception{
		String wfID=UUID.randomUUID().toString();
		ActivityGroup job=new ActivityGroup("1234",wfID);
		int targetValue=2;
		List<Activity>as = new ArrayList<>();
		as.add(new ModifyVariableActivity("a1", job.getWorkflowID(), "test1", "test1++"));
		job.setActivities(as);
		job.init();
		
		WorkflowContainer attr=new WorkflowContainer();
		attr.build(job);
		PEConfig.getInstance().getPersistence().write(attr);
		Action action=new Action();
		action.setUUID(job.getWorkflowID());
		action.setAjd(job);
		action.setType(ActivityGroup.ACTION_TYPE);
		ProcessVariables vars=new ProcessVariables();
		//manually set initial value
		vars.put("test1",Integer.valueOf(1));
		action.getProcessingContext().put(ProcessVariables.class, vars);
		xnjs.get(Manager.class).add(action, null);
		waitForDone(wfID);
		//check that we have a variable in the PC with the requested value
		action=xnjs.get(InternalManager.class).getAction(wfID);
		assert(action!=null);
		ProcessVariables pv=action.getProcessingContext().get(ProcessVariables.class);
		assert(pv!=null);
		Integer test1=pv.get("test1", Integer.class);
		assert(test1!=null);
		assert(test1.intValue()==targetValue);
	}
	
	@Test
	public void testModifyVariableConcurrently()throws Exception{
		String wfID=UUID.randomUUID().toString();
		ActivityGroup job=new ActivityGroup("1234",wfID);
		
		List<Activity>as=new ArrayList<Activity>();
		as.add(new ModifyVariableActivity("modify1",job.getWorkflowID(),"test1","test1++"));
		as.add(new ModifyVariableActivity("modify2",job.getWorkflowID(),"test2","test2++"));
		job.setActivities(as);
		job.init();
		
		WorkflowContainer attr=new WorkflowContainer();
		attr.build(job);
		PEConfig.getInstance().getPersistence().write(attr);
		Action action=new Action();
		action.setUUID(job.getWorkflowID());
		action.setAjd(job);
		action.setType(ActivityGroup.ACTION_TYPE);
		ProcessVariables vars=new ProcessVariables();
		//manually set initial values
		vars.put("test1",Integer.valueOf(1));
		vars.put("test2",Integer.valueOf(1));
		action.getProcessingContext().put(ProcessVariables.class, vars);
		xnjs.get(Manager.class).add(action, null);
		waitForDone(wfID);
		
		//check that we have a variable in the PC with the requested value
		action=xnjs.get(InternalManager.class).getAction(wfID);
		assert(action!=null);
		ProcessVariables pv=action.getProcessingContext().get(ProcessVariables.class);
		assert(pv!=null);
		Integer test1=pv.get("test1", Integer.class);
		assert(test1!=null);
		assert test1.intValue()==2: "got wrong value for <test2>: "+test1.intValue();
		Integer test2=pv.get("test2", Integer.class);
		assert(test2!=null);
		assert(test2.intValue()==2): "got wrong value for <test2>: "+test2.intValue();
	}
	
	@Test()
	public void testSecurityOfModifyVariable()throws Exception{
		String wfID = UUID.randomUUID().toString();
		PEWorkflow job = new PEWorkflow(wfID);
		List<Activity>as = new ArrayList<>();
		
		// malicious script
		as.add(new ModifyVariableActivity("a1",job.getWorkflowID(),"test1","System.exit(1)"));
		
		job.setActivities(as);
		job.init();
		
		WorkflowContainer attr=new WorkflowContainer();
		attr.build(job);
		PEConfig.getInstance().getPersistence().write(attr);
		Action action=new Action();
		action.setUUID(job.getWorkflowID());
		action.setAjd(job);
		action.setType(ActivityGroup.ACTION_TYPE);
		ProcessVariables vars=new ProcessVariables();
		//manually set initial value
		vars.put("test1",Integer.valueOf(1));
		action.getProcessingContext().put(ProcessVariables.class, vars);
		xnjs.get(Manager.class).add(action, null);
		waitForDone(wfID);
		
		//check that we have action failed
		action=xnjs.get(InternalManager.class).getAction(wfID);
		assert(action!=null);
		assert action.getResult().getStatusCode()==ActionResult.NOT_SUCCESSFUL;
	}

}