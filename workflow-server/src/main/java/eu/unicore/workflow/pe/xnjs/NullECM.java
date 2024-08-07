package eu.unicore.workflow.pe.xnjs;

import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.IExecutionContextManager;

public class NullECM implements IExecutionContextManager{

	@Override
	public void initialiseContext(Action action) throws ExecutionException {}

	@Override
	public String createUSpace(Action action) throws ExecutionException {
		return null;
	}

	public String createUSpace(Action action, String baseDirectory) throws ExecutionException {
		return null;
	}

	public void destroyUSpace(Action action) throws ExecutionException {
	}

	@Override
	public void initialiseChildContext(Action parentAction, Action childAction) throws ExecutionException {}

}
