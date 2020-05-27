package eu.unicore.workflow.pe.model;


/**
 * Activity to modify a workflow variable
 *
 * @author schuller
 */
public class ModifyVariableActivity extends Activity{

	private static final long serialVersionUID=1;
	
	public static final String ACTION_TYPE="UNICORE_WORKFLOW_MODIFY_VARIABLE_ACTIVITY";
	
	private final String variableName;
	private final String script;
	
	/**
	 * create a new ModifyVariableActivity
	 * 
	 * @param id -  the activity ID
	 * @param workflowID - the workflowID
	 * @param variableName
	 * @param script - the expression to execute
	 */
	public ModifyVariableActivity(String id, String workflowID, String variableName, String script){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
		this.script=script;
		this.variableName=variableName;
	}

	@Override
	public String getType() {
		return ACTION_TYPE;
	}

	public String getVariableName() {
		return variableName;
	}

	public String getScript() {
		return script;
	}
	
}
