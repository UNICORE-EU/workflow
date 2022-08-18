package eu.unicore.workflow.pe.iterators;

/**
 * thrown by the iterators in case things go wrong
 */
public class IterationException extends Exception {

	private static final long serialVersionUID = 1L;

	public IterationException(Throwable cause) {
		super(cause);
	}

}
