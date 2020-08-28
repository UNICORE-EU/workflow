package eu.unicore.workflow.pe.model;

import eu.unicore.workflow.pe.iterators.Iteration;

/**
 * a top-level workflow to be executed by the process engine
 * 
 * @author schuller
 */
public class PEWorkflow extends ActivityGroup {

	private static final long serialVersionUID = 1L;

	public PEWorkflow(String workflowID) {
		super(workflowID, workflowID, new NullIteration());
	}

	private static class NullIteration extends Iteration{

		private static final long serialVersionUID = 1L;
		
		public String getCurrentValue(){
			return null;
		}
	}
}
