package eu.unicore.workflow.pe.model;

import eu.unicore.util.Log;


/**
 * Information about a workflow activity
 *
 * @author schuller
 */
public abstract class Activity extends ModelBase{

	private static final long serialVersionUID=1;
	
	protected ActivityStatus status;
	
	protected SplitType splitType=SplitType.FOLLOW_ALL_MATCHING;
	
	protected MergeType mergeType=MergeType.SYNCHRONIZE;
	
	Activity(){}
	
	public Activity(String id, String workflowID){
		super(id,workflowID);
	}

	public Activity(String id, String workflowID, Iterate iteration){
		super(id,workflowID,iteration);
	}

	public ActivityStatus getStatus() {
		return status;
	}

	public void setStatus(ActivityStatus status) {
		this.status = status;
	}
	
	public abstract String getType();
	
	public SplitType getSplitType(){
		return splitType;
	}
	
	public void setSplitType(SplitType splitType){
		this.splitType=splitType;
	}
	
	public MergeType getMergeType() {
		return mergeType;
	}

	public void setMergeType(MergeType mergeType) {
		this.mergeType = mergeType;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(id).append(" [type=").append(getType()).append(" status=").append(status).append(" wfID=").append(workflowID);
		sb.append(" obj=").append(super.toString());
		sb.append("]");
		return sb.toString();
	}
	
	public Activity clone(){
		try{
			Activity cloned=(Activity)super.clone();
			return cloned;
		}catch(CloneNotSupportedException ne){
			Log.logException("Clone of "+this.getClass().getName()+" not supported", ne);
			return this;
		}
	}
	
	/**
	 * define semantics of multiple outgoing transitions
	 */
	public static enum SplitType{
		
		/**
		 * only the first matching transition is followed
		 */
		FOLLOW_FIRST_MATCHING,
		
		/**
		 * all matching transition are followed
		 */
		FOLLOW_ALL_MATCHING
	};
	
	/**
	 * define semantics of multiple incoming transitions
	 */
	public static enum MergeType{
		
		/**
		 * synchronization is performed (default!)
		 */
		SYNCHRONIZE,
		
		/**
		 * incoming transition need not be synchronized
		 */
		MERGE
	};
}
