package eu.unicore.workflow.pe;

import java.util.Calendar;

import org.junit.Test;

public class TestEvaluator {

	@Test
	public void testDateFormat(){
		String test="2000-01-01 12:30";
		assert(new Evaluator("",null).after(test));
		assert(!new Evaluator("",null).before(test));
	}
	
	@Test
	public void testDateFormat2()throws InterruptedException{
		Calendar c=Calendar.getInstance();
		Evaluator e=new Evaluator("",null);
		String test=e.getFormatted(c);
		System.out.println(test);
		Thread.sleep(2000);
		assert(e.after(test));
	}
	
}
