package eu.unicore.workflow.pe.iterators;

import org.junit.Test;

import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class TestIteration {

	 @Test
	 public void test1Variable(){
		 ProcessVariables vars=new ProcessVariables();
		 vars.put("TEST", "123");
		 Iteration i=new Iteration();
		 i.setBase("${TEST}");
		 i.next(vars);
		 assert("123".equals(i.getResolvedBase()));
	 }

	 @Test
	 public void test2Variables(){
		 ProcessVariables vars=new ProcessVariables();
		 vars.put("TEST", "123");
		 vars.put("FOO", "456");
		 Iteration i=new Iteration();
		 i.setBase("${TEST}:::${FOO}");
		 i.next(vars);
		 assert("123:::456".equals(i.getResolvedBase()));
	 }
	 
	 @Test
	 public void testMixedContent(){
		 ProcessVariables vars=new ProcessVariables();
		 vars.put("TEST", "123");
		 vars.put("FOO", "456");
		 Iteration i=new Iteration();
		 i.setBase("abc${TEST}:::SomeOtherFOOContent:::${FOO}abc");
		 i.next(vars);
		 assert("abc123:::SomeOtherFOOContent:::456abc".equals(i.getResolvedBase()));
	 }
	 
	 @Test(expected=IllegalStateException.class)
	 public void testForIllegalState(){
		 ProcessVariables vars=new ProcessVariables();
		 vars.put("TEST", "123");
		 Iteration i=new Iteration();
		 i.setBase("foo");
		 i.getCurrentValue();
	 }

}
