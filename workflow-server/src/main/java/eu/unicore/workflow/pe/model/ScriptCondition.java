package eu.unicore.workflow.pe.model;

import eu.unicore.workflow.pe.util.ScriptSandbox;
import eu.unicore.workflow.pe.xnjs.Constants;
import groovy.lang.GroovyShell;

/**
 * Evaluate a condition that is based on a Groovy script
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
	 * @param script - the beanshell expression to evaluate
	 */
	public ScriptCondition(String id, String workflowID, String script) {
		super(id, workflowID);
		if(script.trim().endsWith(";")){
			this.script=script.trim();	
		}else{
			this.script=script.trim()+";";
		}
	}

	public synchronized boolean evaluate()throws EvaluationException{
		Object o=null;
		try{
			StringBuilder sb = new StringBuilder();
			GroovyShell shell=new GroovyShell();
			for(String key: processVariables.keySet()){
				shell.setVariable(key, processVariables.get(key));
			}
			shell.setVariable(Constants.VAR_KEY_CURRENT_TOTAL_ITERATION, iterationValue);
			ScriptSandbox sandbox=new ScriptSandbox();
			sb.append(script);
			o=sandbox.eval(shell,sb.toString());
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
