package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.util.ScriptSandbox;
import groovy.lang.GroovyShell;

/**
 * Modifies a named variable <br/>
 * 
 * @author schuller
 */
public class ModificationActivityProcessor extends ProcessorBase{

	private static final Logger logger=Log.getLogger(Log.SERVICES,ModificationActivityProcessor.class);
	
	public ModificationActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		ModifyVariableActivity activity=(ModifyVariableActivity)action.getAjd();
		String name = activity.getVariableName();
		String myIteration=(String)action.getProcessingContext().get(PV_KEY_ITERATION);
		logger.info("Start processing activity <"+activity.getID()+"> in iteration <"+myIteration+">");
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		if(vars==null){
			vars=new ProcessVariables();
			action.getProcessingContext().put(ProcessVariables.class,vars);
		}
		try{
			GroovyShell interpreter = new GroovyShell();
			prepareInterpreter(interpreter, vars, myIteration);
			logger.debug("Executing script "+activity.getScript());
	        new ScriptSandbox().eval(interpreter, activity.getScript());
	        readResults(interpreter, vars, name);
			setToDoneSuccessfully();
		}
		catch(Exception iae){
			String msg = Log.createFaultMessage("Script evaluation failed", iae);
			reportError("EvaluationError", msg);
			setToDoneAndFailed("Script evaluation failed: " + msg);
		}
		
	}

	private void prepareInterpreter(GroovyShell interpreter, ProcessVariables vars, String myIteration){
		logger.debug("Context has "+vars.size()+" entries");
		for(String key: vars.keySet()){
			Object val=vars.get(key);
			logger.debug("Context: "+key+"="+String.valueOf(val)+" variableType="+val.getClass().getName());
			interpreter.setVariable(key, val);
		}
		interpreter.setVariable(VAR_KEY_CURRENT_TOTAL_ITERATION,myIteration);
	}
	
	/*
	 * copies only the named variable, preventing side effects
	 */
	private void readResults(GroovyShell interpreter, ProcessVariables vars, String varName){
		if(varName==null)return;
		Object res=interpreter.getVariable(varName);
		if(res!=null){
			vars.put(varName,res);
			vars.markModified(varName);
		}
	}
	
}
