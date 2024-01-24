package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.util.VariableUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;

/**
 * Processes a variable declaration
 * @author schuller
 */
public class DeclarationActivityProcessor extends ProcessorBase{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, DeclarationActivityProcessor.class);
	
	public DeclarationActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
		DeclareVariableActivity activity=(DeclareVariableActivity)action.getAjd();
		String myIteration=(String)action.getProcessingContext().get(PV_KEY_ITERATION);
		logger.info("Start processing activity <{}> in iteration <{}>", activity.getID(), myIteration);
		ProcessVariables vars=action.getProcessingContext().get(ProcessVariables.class);
		if(vars==null){
			vars=new ProcessVariables();
			action.getProcessingContext().put(ProcessVariables.class,vars);
		}
		try{
			vars.put(activity.getVariableName(), VariableUtil.create(activity.getVariableType(), activity.getInitialValue()));
			// markModified so variable will be visible in parent group - TODO also in parent of parent, need scopes!
			vars.markModified(activity.getVariableName());
			setToDoneSuccessfully();
		}
		catch(IllegalArgumentException iae){
			reportError("InvalidVariableDeclaration","Invalid variable declaration: "+iae.getMessage());
			setToDoneAndFailed("Invalid variable declaration: "+iae);
		}
		
	}
	
}
