package eu.unicore.workflow.pe.model;


/**
 * Activity to declare a workflow variable
 *
 * @author schuller
 */
public class DeclareVariableActivity extends Activity{

	private static final long serialVersionUID=1;
	
	public static final String ACTION_TYPE="UNICORE_WORKFLOW_DECLARE_VARIABLE_ACTIVITY";

	private final String variableName;
	private final String initialValue;
	private final String variableType;
	
	/**
	 * create a new DeclareVariableActivity
	 * 
	 * @param id -  the activity ID
	 * @param workflowID - the workflowID
	 * @param variableName
	 * @param variableType
	 * @param initialValue
	 */
	public DeclareVariableActivity(String id, String workflowID, String variableName, String variableType, String initialValue){
		super(id,workflowID);
		status=ActivityStatus.CREATED;
		this.initialValue=initialValue;
		this.variableName=variableName;
		this.variableType=variableType;
	}

	@Override
	public String getType() {
		return ACTION_TYPE;
	}

	public String getVariableName() {
		return variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public String getInitialValue() {
		return initialValue;
	}
	
}
