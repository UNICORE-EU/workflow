package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.Transition;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.xnjs.TestBase;

public class TestStatusStorage extends TestBase {

	private PEWorkflow buildJob(String workflowID)throws ProcessingException{
		PEWorkflow job=new PEWorkflow(workflowID);
		List<Activity>as=new ArrayList<Activity>();
		as.add(new TestActivity("a1",job.getWorkflowID()));
		as.add(new TestActivity("a2",job.getWorkflowID()));
		Transition t=new Transition("a1->a2",job.getWorkflowID(),"a1","a2");
		job.setActivities(as);
		job.setTransitions(t);
		job.init();
		return job;
	}
	
	@Test
	public void testBuildSubflowInfo()throws Exception{
		SubflowContainer attr=new SubflowContainer();
		String wfID=UUID.randomUUID().toString();
		PEWorkflow job=buildJob(wfID);
		attr.build(job);
		List<PEStatus> a1=attr.getActivityStatus("a1");
		assert(a1!=null);
		List<PEStatus> a2=attr.getActivityStatus("a2");
		assert(a2!=null);
	}

	@Test
	public void testStoreSubflowInfo()throws Exception{
		String wfID=UUID.randomUUID().toString();
		
		PEWorkflow job=buildJob(wfID);
		
		PEConfig.getInstance().getProcessEngine().process(job, null);
		
		waitForDone(job.getWorkflowID());
		
		SubflowContainer results=PEConfig.getInstance().getPersistence().read(wfID);
		assert(results!=null);
		List<PEStatus> a1Status=results.getActivityStatus("a1");
		assert(a1Status!=null);
		
		//check that correct PEStatus has been stored
		assert(a1Status.size()==1);
		PEStatus p1=a1Status.get(0);
		assert(p1!=null);
	}
	
}