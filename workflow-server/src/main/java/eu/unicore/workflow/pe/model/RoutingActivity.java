package eu.unicore.workflow.pe.model;


/**
 * No-op activity used for various purposes, such as explicit start, split and join
 *
 * @author schuller
 */
public class RoutingActivity extends Activity{

	private static final long serialVersionUID=1;
	
	public static final String ACTION_TYPE="UNICORE_WORKFLOW_ROUTING_ACTIVITY";
	
	/**
	 * create a new RoutingActivity
	 * 
	 * @param id -  the activity ID
	 * @param workflowID - the workflowID
	 */
	public RoutingActivity(String id, String workflowID){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
	}

	@Override
	public String getType() {
		return ACTION_TYPE;
	}
	
}
