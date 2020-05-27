package eu.unicore.workflow.pe.util;

import org.apache.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.pe.xnjs.Constants;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;
import groovy.lang.GroovyShell;

/**
 * evaluates Groovy scripts to generate values sets for iterators 
 * 
 * @author schuller
 */
public class ScriptEvaluator {

	private static final Logger logger=Log.getLogger(Log.SERVICES,ScriptEvaluator.class);

	private final ScriptSandbox sandbox; 
	
	public ScriptEvaluator(){
		this.sandbox=new ScriptSandbox();
	}
	
	/**
	 * Evaluate a script (which is expected to have a boolean result)
	 * 
	 * @param script
	 * @param processVariables
	 * @throws IllegalArgumentException
	 */
	public boolean evaluate(String script, ProcessVariables processVariables)throws IllegalArgumentException{
		if(processVariables==null){
			throw new IllegalArgumentException("Process variables can't be null.");
		}
		if(script==null){
			throw new IllegalArgumentException("Expression can't be null.");
		}
		GroovyShell interpreter = new GroovyShell();
		prepareInterpreter(interpreter, processVariables);
		logger.debug("Evaluating expression: "+script);
		Object o=sandbox.eval(interpreter,script);
		if(o instanceof Boolean){
			return ((Boolean)o).booleanValue();
		}
		else throw new IllegalArgumentException("Conditional expression <"+script+"> does not evaluate to a boolean!");
	}
	
	/**
	 * Run a script to evaluate the value of the given variable
	 * 
	 * @param variableName - the name of the target variable
	 * @param processVariables - the set of variables (will not be modified)
	 * 
	 * @return the value of the named variable after executing the script
	 * @throws IllegalArgumentException
	 */
	public Object evaluate(String script, String variableName, ProcessVariables processVariables)throws IllegalArgumentException{
		if(processVariables==null){
			throw new IllegalArgumentException("Process variables can't be null.");
		}
		if(script==null){
			throw new IllegalArgumentException("Expression can't be null.");
		}
		GroovyShell interpreter = new GroovyShell();
		prepareInterpreter(interpreter, processVariables);
		if(logger.isDebugEnabled()){
			logger.debug("Evaluating expression: "+script+" with context "+processVariables);
		}
		sandbox.eval(interpreter,script);
		return interpreter.getVariable(variableName);
	}
	
	/**
	 * run a script and return the result
	 * 
	 * @param script
	 * @param processVariables
	 * 
	 * @return the evaluation result
	 */
	public Object evaluateDirect(String script, ProcessVariables processVariables){
		GroovyShell interpreter = new GroovyShell();
		prepareInterpreter(interpreter, processVariables);
		if(logger.isDebugEnabled()){
			logger.debug("Evaluating expression: "+script+" with context "+processVariables);
		}
		return sandbox.eval(interpreter,script);
	}

	public String evaluateToString(String script, String varName, ProcessVariables processVariables)throws  IllegalArgumentException{
		return String.valueOf(evaluate(script, varName, processVariables));
	}

	public Integer evaluateToInteger(String script, String varName, ProcessVariables processVariables)throws IllegalArgumentException{
		return (Integer) evaluate(script, varName, processVariables);
	}


	private void prepareInterpreter(GroovyShell interpreter, ProcessVariables vars){
		logger.debug("Context has "+vars.size()+" entries");
		for(String key: vars.keySet()){
			Object val=vars.get(key);
			logger.debug("Context: "+key+"="+String.valueOf(val)+" variableType="+val.getClass().getName());
			interpreter.setVariable(key, val);
		}
		interpreter.setProperty(Constants.VAR_KEY_CURRENT_TOTAL_ITERATION, null);
	}


}
