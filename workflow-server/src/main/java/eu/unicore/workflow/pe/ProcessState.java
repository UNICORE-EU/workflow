package eu.unicore.workflow.pe;

import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class ProcessState{

	public static enum State {
		UNDEFINED,

		RUNNING,

		SUCCESSFUL,

		ABORTED,

		FAILED,

		HELD,
	}
	
	private State state = State.UNDEFINED;
	
	private ProcessVariables variables;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public ProcessVariables getVariables() {
		return variables;
	}

	public void setVariables(ProcessVariables variables) {
		this.variables = variables;
	}
	
}
