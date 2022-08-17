package eu.unicore.workflow.pe.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Information about an workflow execution activity
 * <ul>
 *   <li>Job definition (JSDL)</li>
 *   <li>Requested job lifetime</li>
 *   <li>Engine waiting behaviour</li>
 *   <li>other properties (unused yet)</li>
 * </ul>
 * 
 * @author schuller
 */
public class JSONExecutionActivity extends Activity{

	private static final long serialVersionUID=1;

	public static final String ACTION_TYPE="UNICORE_WORKFLOW_JSON_ACTIVITY";
	
	public static final String OPTION_IGNORE_FAILURE="IGNORE_FAILURE";
	
	public static final String OPTION_MAX_RESUBMITS="MAX_RESUBMITS";
	
	// whether to not use callbacks from the UNICORE servers to update job status
	public static final String OPTION_NO_NOTIFICATIONS = "DISABLE_NOTIFICATIONS";
	
	private String jobDefinition;
	
	private final Map<String,String>options=new HashMap<>();
	
	//requested job resource lifetime
	private long lifetimeMillis=-1;

	//count (re)submissions of the same workassignment
	private int submissionCounter=-1;

	//blacklist of sites not to submit to
	private Set<String>blacklist=new HashSet<String>();
	
	//understood option names
	public static final Set<String>UNDERSTOOD_OPTIONS;
	
	static{
		HashSet<String> s=new HashSet<String>();
		s.add(OPTION_IGNORE_FAILURE);
		s.add(OPTION_MAX_RESUBMITS);
		s.add(OPTION_NO_NOTIFICATIONS);
		UNDERSTOOD_OPTIONS = Collections.unmodifiableSet(s);
	}
	
	public JSONExecutionActivity(String id, String workflowID){
		super(id, workflowID);
	}
	
	public JSONObject getJobDefinition() {
		try{
			return new JSONObject(jobDefinition);
		}catch(JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public void setJobDefinition(JSONObject jobDefinition) {
		this.jobDefinition = jobDefinition.toString();
	}
	
	public String getType(){
		return ACTION_TYPE;
	}

	/**
	 * returns the submission counter for the last submitted workassignment. 
	 * Should be incremented before submission of the next WA
	 */
	public int getSubmissionCounter() {
		return submissionCounter;
	}

	public void incrementSubmissionCounter() {
		submissionCounter++;
	}
	
	/**
	 * gets an option
	 * 
	 * @param key - the option to retrieve
	 * @return the option value
	 */
	public String getOption(String key){
		return options.get(key);
	}
	
	/**
	 * sets an option
	 * 
	 * @param key
	 * @param value
	 * @return the value previously mapped to the key (see also Map.put())
	 */
	public String setOption(String key, String value){
		return options.put(key,value);
	}
	
	/**
	 * get the requested job lifetime (or -1 if none set) 
	 */
	public long getLifetimeMillis() {
		return lifetimeMillis;
	}

	/**
	 * set the requested job lifetime in millis
	 * 
	 * @param lifetimeMillis
	 */
	public void setLifetimeMillis(long lifetimeMillis) {
		this.lifetimeMillis = lifetimeMillis;
	}

	public void addToBlackList(String site){
		blacklist.add(site);
	}
	
	public Set<String>getBlacklist(){
		return blacklist;
	}
	
	/**
	 * should a failure of this activity be ignored?
	 */
	public boolean isIgnoreFailure(){
		return Boolean.parseBoolean(options.get(OPTION_IGNORE_FAILURE));
	}
	
}