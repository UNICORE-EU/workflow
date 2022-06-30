package eu.unicore.workflow.pe.xnjs;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.processors.DefaultProcessor;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.model.Activity;

/**
 * Processes a routing (no-op) activity<br/>
 * 
 * @author schuller
 */
public class RoutingActivityProcessor extends DefaultProcessor implements Constants{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, RoutingActivityProcessor.class);
	
	public RoutingActivityProcessor(XNJS configuration) {
		super(configuration);
	}

	@Override
	protected void handleCreated() throws ProcessingException {
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"Success.",0));
		String myIteration=(String)action.getProcessingContext().get(PV_KEY_ITERATION);
		if(logger.isDebugEnabled()){
			Activity activity=(Activity)action.getAjd();
			logger.debug("Processed routing activity <{}> in iteration <{}>", activity.getID(), myIteration);
		}
	}
	
}
