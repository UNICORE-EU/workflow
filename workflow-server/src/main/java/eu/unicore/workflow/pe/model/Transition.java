package eu.unicore.workflow.pe.model;

import eu.unicore.persist.util.Wrapper;

/**
 * A transition links parts of the workflow. It can
 * be <em>conditional</em> or <em>unconditional</em>.
 *
 * @author schuller
 */
public class Transition extends ModelBase {

	private static final long serialVersionUID=1;
	
	private Wrapper<Condition> condition;
	
	final String from,to;
	
	/**
	 * create a new conditional transition
	 * 
	 * @param id - the id of this transition
	 * @param workflowID - the id of the parent workflow
	 * @param from - the start activity 
	 * @param to - the end activity
	 * @param condition - the condition (may be null)
	 */
	public Transition(String id, String workflowID, String from, String to, Condition condition) {
		super(id,workflowID);
		this.condition=condition!=null?new Wrapper<Condition>(condition):null;
		this.to=to;
		this.from=from;
	}

	/**
	 * create a new unconditional transition
	 * 
	 * @param id - the id of this transition
	 * @param workflowID - the id of the parent workflow
	 * @param from - the start activity 
	 * @param to - the end activity
	 */
	public Transition(String id, String workflowID, String from, String to) {
		this(id,workflowID,from,to,null);
	}
	
	/**
	 * checks whether this is an unconditional transition
	 */
	public boolean isConditional(){
		return condition!=null;
	}
	
	/**
	 * return a user friendly representation
	 */
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(from).append("->").append(to);
		if(isConditional()){
			sb.append(" when ").append(String.valueOf(condition));
		}
		sb.append("[").append(id).append(" in ").append(workflowID).append("]");
		return sb.toString();
	}

	public Condition getCondition() {
		return condition!=null?condition.get():null;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}
	
	public Transition clone()throws CloneNotSupportedException{
		Transition cloned=(Transition)super.clone();
		if(this.condition!=null){
			cloned.condition=new Wrapper<Condition>(this.condition.get());
		}
		return cloned;
	}

}
