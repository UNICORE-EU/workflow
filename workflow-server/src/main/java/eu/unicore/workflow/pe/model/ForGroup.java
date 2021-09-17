package eu.unicore.workflow.pe.model;



/**
 * "for-each" group
 * 
 * @author schuller
 */
public class ForGroup extends SingleBodyContainer {
	
	private static final long serialVersionUID=1;

	public static final String ACTION_TYPE="UNICORE_WORKFLOW_FOR_EACH";

	/**
	 * property for defining the maximum number of concurrent activities per for-each group
	 * (hard limit)
	 */
	public static final String[] PROPERTY_MAX_CONCURRENT_ACTIVITIES =
	  new String[] { "unicore.workflow.forEach.maxConcurrentActivities",
			  "maxConcurrentActivities"
	  };

	private int maxConcurrentActivities = -1;
	
	/**
	 * limit on concurrent activities
	 */
	public int getMaxConcurrentActivities() {
		return maxConcurrentActivities;
	}

	public void setMaxConcurrentActivities(int maxConcurrentActivities) {
		this.maxConcurrentActivities = maxConcurrentActivities;
	}
	
	public ForGroup(String id,String workflowID, Iterate iteration, ActivityGroup body) {
		super(id,workflowID,iteration,body);
	}
	
	public ForGroup(String id,String workflowID, ActivityGroup body) {
		super(id,workflowID,body);
	}

	@Override
	public String getType(){
		return ACTION_TYPE;
	}

	@Override
	public Activity getActivity(String id) {
		return super.getActivity(id);
	}

}
