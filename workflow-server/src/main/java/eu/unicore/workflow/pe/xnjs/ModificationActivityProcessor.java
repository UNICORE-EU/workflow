package eu.unicore.workflow.pe.xnjs;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.util.ScriptEvaluator;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.ContextFunctions;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;

/**
 * Modifies a named variable <br/>
 * 
 * @author schuller
 */
public class ModificationActivityProcessor extends ProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, ModificationActivityProcessor.class);
	
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
		logger.debug("Start processing activity <{}> in iteration <{}>", activity.getID(), myIteration);
		ProcessVariables pv = action.getProcessingContext().get(ProcessVariables.class);
		if(pv==null){
			pv = new ProcessVariables();
			action.getProcessingContext().put(ProcessVariables.class, pv);
		}
		try{
			Map<String, Object> vars = pv.asMap();
			vars.put(VAR_KEY_CURRENT_TOTAL_ITERATION, myIteration);
			ContextFunctions contextFunctions = new ContextFunctions(getParentWorkflowID(), myIteration);
			logger.debug("Executing script {}", activity.getScript());
			ScriptEvaluator.evaluate(activity.getScript(), vars, contextFunctions);
	        readResult(vars, pv, name);
			setToDoneSuccessfully();
		}
		catch(Exception iae){
			String msg = Log.createFaultMessage("Script evaluation failed", iae);
			reportError("EvaluationError", msg);
			setToDoneAndFailed("Script evaluation failed: " + msg);
		}
		
	}

	/*
	 * copies only the named variable, preventing side effects
	 */
	private void readResult(Map<String, Object> vars, ProcessVariables pv, String varName){
		if(varName==null)return;
		Object res = vars.get(varName);
		if(res!=null){
			pv.put(varName,res);
			pv.markModified(varName);
		}
	}
	
}
