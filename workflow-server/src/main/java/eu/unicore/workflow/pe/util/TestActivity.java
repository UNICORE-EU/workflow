package eu.unicore.workflow.pe.util;

import de.fzj.unicore.persist.util.Wrapper;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;


/**
 * Special no-op activity for testing purposes
 *
 * @author schuller
 */
public class TestActivity extends JSONExecutionActivity {

	private static final long serialVersionUID=1;
	
	private final int delay;
	
	private boolean useCallback=false;

	private boolean waitForExternalCallback=false;

	public TestActivity(String id, String workflowID, Iterate iter){
		super(id,workflowID);
		iteration=new Wrapper<Iterate>(iter);
		status=ActivityStatus.CREATED;
		delay=-1;
	}

	public TestActivity(String id, String workflowID){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
		delay=-1;
	}

	public TestActivity(String id, String workflowID, int delay){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
		this.delay=delay;
	}

	public String getType(){
		return "TESTING";
	}

	public int getDelay(){
		return delay;
	}

	public boolean waitForExternalCallback() {
		return waitForExternalCallback;
	}

	public void setWaitForExternalCallback(boolean waitForCallback) {
		this.waitForExternalCallback = waitForCallback;
	}
	
	public boolean useCallback() {
		return useCallback;
	}

	/**
	 * whether to use the callback to notify that the activity is finished
	 * @param useCallback
	 */
	public void setUseCallback(boolean useCallback) {
		this.useCallback = useCallback;
	}
	
	public boolean isIgnoreFailure(){
		return false;
	}

}
