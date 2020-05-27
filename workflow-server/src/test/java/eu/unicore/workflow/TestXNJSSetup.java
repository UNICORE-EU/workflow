package eu.unicore.workflow;

import java.util.List;

import org.junit.Test;

import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.xnjs.ActivityGroupProcessor;
import eu.unicore.workflow.pe.xnjs.TestingActivityProcessor;
import eu.unicore.workflow.xnjs.TestBase;

public class TestXNJSSetup extends TestBase {

	@Test
	public void test1(){
		assert(xnjs.haveProcessingFor(ActivityGroup.ACTION_TYPE));
		List<String> p=xnjs.getProcessorChain(ActivityGroup.ACTION_TYPE);
		assert(p.size()==1);
		assert(ActivityGroupProcessor.class.getName().equals(p.get(0)));
	}
	
	@Test
	public void test2(){
		assert(xnjs.haveProcessingFor(TestingActivityProcessor.ACTION_TYPE));
		List<String> p = xnjs.getProcessorChain(TestingActivityProcessor.ACTION_TYPE);
		assert(p.size()==1);
		assert(TestingActivityProcessor.class.getName().equals(p.get(0)));
	}
	
}
