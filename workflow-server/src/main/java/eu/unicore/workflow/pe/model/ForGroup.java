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
	 * default maximum number of concurrent activities per for-each group
	 */
	public static int DEFAULT_MAX_CONCURRENT_ACTIVITIES=20;

	/**
	 * property for defining the maximum number of concurrent activities per for-each group
	 */
	public static final String PROPERTY_MAX_CONCURRENT_ACTIVITIES="unicore.workflow.forEach.maxConcurrentActivities";

	private int maxConcurrentActivities=DEFAULT_MAX_CONCURRENT_ACTIVITIES;
	
	public int getMaxConcurrentActivities() {
		return maxConcurrentActivities;
	}

	public void setMaxConcurrentActivities(int maxConcurrentActivities) {
		this.maxConcurrentActivities = maxConcurrentActivities;
	}

	public ForGroup(String id,String workflowID, Iterate iteration, Activity body) {
		super(id,workflowID,iteration,body);
	}
	
	public ForGroup(String id,String workflowID, Activity body) {
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
