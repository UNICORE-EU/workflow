package eu.unicore.workflow.pe.model;

import eu.unicore.util.Log;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * basic condition class
 * 
 * @author schuller
 */
public abstract class Condition extends ModelBase{
	
	private static final long serialVersionUID = 1L;
	
	protected transient ProcessVariables processVariables;
	
	protected transient String iterationValue;
	
	public Condition(String id, String workflowID) {
		super(id, workflowID);
	}

	public abstract boolean evaluate() throws EvaluationException;

	public ProcessVariables getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(ProcessVariables vars) {
		this.processVariables = vars;
	}
	
	public void setIterationValue(String iter){
		this.iterationValue=iter;
	}
	
	public Condition clone(){
		try{
			return (Condition)super.clone();
		}catch(CloneNotSupportedException ce){
			Log.logException("Clone of "+this.getClass().getName()+" not supported", ce);
			return this;
		}
	}
}
