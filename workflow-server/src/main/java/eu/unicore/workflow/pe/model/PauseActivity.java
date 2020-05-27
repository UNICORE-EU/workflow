package eu.unicore.workflow.pe.model;


/**
 * PAUSE activity
 *
 * @author schuller
 */
public class PauseActivity extends Activity{

	private static final long serialVersionUID=1;
	
	public static final String ACTION_TYPE="UNICORE_WORKFLOW_PAUSE_ACTIVITY";
	
	// seconds
	private long sleepTime;
	
	public void setSleepTime(long sleepTime){
		this.sleepTime = sleepTime;
	}
	
	public long getSleepTime(){
		return sleepTime;
	}

	/**
	 * create a new HoldActivity
	 * 
	 * @param id -  the activity ID
	 * @param workflowID - the workflowID
	 */
	public PauseActivity(String id, String workflowID){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
	}

	@Override
	public String getType() {
		return ACTION_TYPE;
	}
	
}
