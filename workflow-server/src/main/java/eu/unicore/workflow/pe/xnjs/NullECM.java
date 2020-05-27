package eu.unicore.workflow.pe.xnjs;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;

public class NullECM implements IExecutionContextManager{

	public ExecutionContext getContext(Action action) throws ExecutionException {
		return null;
	}

	public String createUSpace(Action action, String baseDirectory)
			throws ExecutionException {
		return null;
	}

	public void destroyUSpace(Action action) throws ExecutionException {
	}

	public ExecutionContext createChildContext(Action parentAction,
			Action childAction) throws ExecutionException {
		return null;
	}

}
