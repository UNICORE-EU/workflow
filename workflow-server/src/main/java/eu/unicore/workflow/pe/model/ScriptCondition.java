package eu.unicore.workflow.pe.model;

import java.util.Map;

import de.fzj.unicore.xnjs.util.ScriptEvaluator;
import eu.unicore.workflow.pe.ContextFunctions;
import eu.unicore.workflow.pe.xnjs.Constants;

/**
 * Evaluate a condition that is based on a script
 * 
 * @author schuller
 */
public class ScriptCondition extends Condition{
	
	private static final long serialVersionUID = 1L;
	
	private final String script;

	/**
	 * create a new ScriptCondition
	 * @param id -  the ID of this condition
	 * @param workflowID - the workflow ID
	 * @param script - the expression to evaluate
	 */
	public ScriptCondition(String id, String workflowID, String script) {
		super(id, workflowID);
		this.script = script;
	}

	public synchronized boolean evaluate()throws EvaluationException{
		Object o=null;
		try{
			Map<String, Object> vars = processVariables.asMap();
			vars.put(Constants.VAR_KEY_CURRENT_TOTAL_ITERATION, iterationValue);
			o = ScriptEvaluator.evaluate(script, vars, new ContextFunctions(workflowID, iterationValue));
			if(o instanceof Boolean)return (Boolean)o;
		}
		catch(Exception ex){
			throw new EvaluationException("Error evaluating condition with id <"+
					id+"> in iteration <"+iterationValue+">",ex);
		}
		throw new EvaluationException("Error evaluating condition with id <"+
				id+">: result is not a boolean.");
	}
	
	public String getScript(){
		return script;
	}

}
