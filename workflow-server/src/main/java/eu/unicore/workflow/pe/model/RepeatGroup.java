package eu.unicore.workflow.pe.model;

import de.fzj.unicore.persist.util.Wrapper;



/**
 * "Repeat-Until" group
 * 
 * @author schuller
 */
public class RepeatGroup extends SingleBodyContainer {
	
	private static final long serialVersionUID=1;

	public static final String ACTION_TYPE="UNICORE_WORKFLOW_REPEAT_UNTIL";

	private final Wrapper<Condition> condition;
	
	public RepeatGroup(String id,String workflowID, Iterate iteration, Activity body, Condition condition) {
		super(id,workflowID,iteration,body);
		this.condition=new Wrapper<Condition>(condition);
	}
	
	public RepeatGroup(String id,String workflowID, Activity body, Condition condition) {
		super(id,workflowID,body);
		this.condition=new Wrapper<Condition>(condition);
	}
	
	@Override
	public String getType(){
		return ACTION_TYPE;
	}

	public Condition getCondition(){
		return condition!=null?condition.get():null;
	}
	
}
