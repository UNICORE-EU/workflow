package eu.unicore.workflow.pe.model;

import de.fzj.unicore.persist.util.Wrapper;



/**
 * "While-Do" group
 * 
 * @author schuller
 */
public class WhileGroup extends SingleBodyContainer {
	
	private static final long serialVersionUID=1;

	public static final String ACTION_TYPE="UNICORE_WORKFLOW_WHILE";

	private final Wrapper<Condition> condition;
	
	public WhileGroup(String id,String workflowID, Iterate iteration, ActivityGroup body, Condition condition) {
		super(id,workflowID,iteration,body);
		this.condition = new Wrapper<>(condition);
	}
	
	public WhileGroup(String id,String workflowID, ActivityGroup body, Condition condition) {
		super(id,workflowID,body);
		this.condition = new Wrapper<>(condition);
	}

	@Override
	public String getType(){
		return ACTION_TYPE;
	}
	
	public Condition getCondition(){
		return condition!=null?condition.get():null;
	}
	
	public WhileGroup clone(){
		WhileGroup cloned=(WhileGroup)super.clone();
		return cloned;
	}

}
