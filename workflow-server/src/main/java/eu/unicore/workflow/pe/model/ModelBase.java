package eu.unicore.workflow.pe.model;

import java.io.Serializable;

import de.fzj.unicore.persist.util.Wrapper;

import eu.unicore.workflow.pe.iterators.Iteration;

/**
 * Base class for workflow elements, having an identifier, a parent workflow ID,
 * and an {@link Iterate} that tracks repeated execution of this workflow element
 *
 * @author schuller
 */
public class ModelBase implements Cloneable, Serializable {

	private static final long serialVersionUID=1;
	
	protected final String id;

	protected final String workflowID;

	protected Wrapper<Iterate> iteration;
	
	public ModelBase(){
		this(null,null);
	}
	
	/**
	 * construct a new model object
	 * @param id
	 * @param workflowID
	 */
	public ModelBase(String id, String workflowID){
		this(id,workflowID,new Iteration());
	}
	
	public ModelBase(String id, String workflowID, Iterate iteration){
		this.id=id;
		this.workflowID=workflowID;
		this.iteration=new Wrapper<Iterate>(iteration);
	}
	
	public String getID(){
		return id;
	}

	public Iterate getIterate(){
		return iteration!=null?iteration.get():null;
	}

	public void setIterate(Iterate iteration){
		this.iteration=new Wrapper<Iterate>(iteration);
	}

	public String getWorkflowID(){
		return workflowID;
	}

	public ModelBase clone()throws CloneNotSupportedException{
		ModelBase cloned=(ModelBase)super.clone();
		if(this.getIterate()!=null){
			cloned.iteration=new Wrapper<Iterate>(this.getIterate());
		}
		return cloned;
	}
	
}
