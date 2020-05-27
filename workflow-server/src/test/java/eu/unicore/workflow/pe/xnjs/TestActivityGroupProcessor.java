package eu.unicore.workflow.pe.xnjs;

import org.junit.After;
import org.junit.Test;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.xnjs.TestBase;

public class TestActivityGroupProcessor extends TestBase {

	@Test
	public void testMaxActivities(){
		xnjs.get(WorkflowProperties.class).setProperty(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP, "10");
		ActivityGroupProcessor agp=new ActivityGroupProcessor(xnjs);
		Action a=new Action();
		agp.setAction(a);
		try{
			agp.incrementCounterAndCheckMaxActivities();
			ActivityCounter c=a.getProcessingContext().get(ActivityCounter.class);
			assert c!=null;
			assert 1==c.get();
		}catch(Exception ex){
			assert 1==0;
		}
	}
	
	
	@Test(expected=ExecutionException.class)
	public void testExceptionWhenCounterExceeded()throws ExecutionException{
		xnjs.get(WorkflowProperties.class).setProperty("maxActivitiesPerGroup", "10");
		WorkflowProperties wp = xnjs.get(WorkflowProperties.class);
		Integer maxProp=wp.getIntValue(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP);
		assert maxProp == 10;
		
		ActivityGroupProcessor agp=new ActivityGroupProcessor(xnjs);
		Action a=new Action();
		agp.setAction(a);
		//now exceed the counter
		for(int i=0;i<100;i++){
			agp.incrementCounterAndCheckMaxActivities();
		}
	}
	
	@Test
	public void testGeneratedErrorMessageWhenCounterExceeded(){
		int n=10;
		xnjs.get(WorkflowProperties.class).setProperty(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP, "10");
		ActivityGroupProcessor agp=new ActivityGroupProcessor(xnjs);
		Action a=new Action();
		agp.setAction(a);
		try{
			//now exceed the counter
			for(int i=0;i<100;i++){
				agp.incrementCounterAndCheckMaxActivities();
			}
		}catch(ExecutionException ee){
			String msg=ee.getMessage();
			assert msg.contains("<"+n+">");
		}
	}

	@After
	public void cleanUp(){
		xnjs.get(WorkflowProperties.class).setProperty(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP, "1000");
	}
	
	@Test
	public void testURLs(){
		String site="https://foo:123/SITE/services/";
		String job="https://foo:123/SITE/services/"+UAS.JMS+"?res=abc";
		String url=job.replaceAll(UAS.JMS+".*", UAS.TSF+"?res=default_target_system_factory");
		assert (site+UAS.TSF+"?res=default_target_system_factory").equals(url) ;
	}
	
}
