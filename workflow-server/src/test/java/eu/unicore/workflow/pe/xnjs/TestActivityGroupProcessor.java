package eu.unicore.workflow.pe.xnjs;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import eu.unicore.uas.UAS;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.xnjs.TestBase;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionException;

public class TestActivityGroupProcessor extends TestBase {

	@Test
	public void testMaxActivities(){
		xnjs.get(WorkflowProperties.class).setProperty(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP, "10");
		ActivityGroupProcessor agp=new ActivityGroupProcessor(xnjs);
		Action a=new Action();
		agp.setAction(a);
		try{
			agp.incrementCounterAndCheckMaxActivities();
			Integer c = (Integer)a.getProcessingContext().get(GroupProcessorBase.ACTIVITY_COUNTER_KEY);
			assert c!=null;
			assert c==1;
		}catch(Exception ex){
			assert 1==0;
		}
	}
	
	
	@Test
	public void testExceptionWhenCounterExceeded()throws ExecutionException{
		xnjs.get(WorkflowProperties.class).setProperty("maxActivitiesPerGroup", "10");
		WorkflowProperties wp = xnjs.get(WorkflowProperties.class);
		Integer maxProp=wp.getIntValue(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP);
		assert maxProp == 10;
		
		ActivityGroupProcessor agp=new ActivityGroupProcessor(xnjs);
		Action a=new Action();
		agp.setAction(a);
		//now exceed the counter
		assertThrows(ExecutionException.class, ()->{
			for(int i=0;i<100;i++){
				agp.incrementCounterAndCheckMaxActivities();
			}
		});
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

	@AfterEach
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
