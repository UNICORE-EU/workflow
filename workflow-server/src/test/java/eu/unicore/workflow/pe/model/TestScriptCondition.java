package eu.unicore.workflow.pe.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.unicore.workflow.pe.xnjs.Constants;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class TestScriptCondition {

	@Test
	public void testSimpleEval()throws EvaluationException{
		String script="true";
		ProcessVariables pv=new ProcessVariables();
		ScriptCondition cond=new ScriptCondition("1","1234",script);
		cond.setProcessVariables(pv);
		assert(cond.evaluate());
	}
	
	@Test
	public void testVarEval1()throws EvaluationException{
		ProcessVariables pv=new ProcessVariables();
		pv.put("X", Integer.valueOf(1));
		String script="X==1";
		ScriptCondition cond=new ScriptCondition("1","1234",script);
		cond.setProcessVariables(pv);
		assert(cond.evaluate());
	}
	
	@Test
	public void testEvalIteration()throws EvaluationException{
		ProcessVariables pv=new ProcessVariables();
		pv.put("X", Integer.valueOf(1));
		String script=Constants.VAR_KEY_CURRENT_TOTAL_ITERATION+".equals(\"999\")";
		ScriptCondition cond=new ScriptCondition("1","1234",script);
		cond.setIterationValue("999");
		cond.setProcessVariables(pv);
		assert(cond.evaluate());
	}
	
	@Test
	public void testEvaluator()throws EvaluationException{
		ProcessVariables pv=new ProcessVariables();
		pv.put("X", Integer.valueOf(1));
		String script="getIteration().equals(\"999\")";
		ScriptCondition cond=new ScriptCondition("1","1234",script);
		cond.setIterationValue("999");
		cond.setProcessVariables(pv);
		assert(cond.evaluate());
	}
	
	@Test
	public void testSecurityCheck()throws EvaluationException{
		ProcessVariables pv=new ProcessVariables();
		pv.put("X", Integer.valueOf(1));
		//malicious code
		String script="System.exit(1);";
		ScriptCondition cond=new ScriptCondition("1","1234",script);
		cond.setProcessVariables(pv);
		assertThrows(EvaluationException.class, ()->{
			cond.evaluate();
		});
	}

}
