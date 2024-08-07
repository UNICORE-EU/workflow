package eu.unicore.workflow.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Holds information about a JSON workflow instance, used during conversion 
 * 
 * @author schuller
 */
public class WorkflowInfo {

	private List<WorkflowInfo> subWorkflowInfos;

	protected java.util.Map<String,JSONObject> subWorkflows;
	protected List<JSONObject> variableDeclarations;
	protected Map<String,JSONObject> activities;
	protected List<JSONObject> transitions;
	
	private boolean isLoop=false;
	private String iteratorName;
	
	public WorkflowInfo(){
		subWorkflowInfos = new ArrayList<>();
		subWorkflows=new HashMap<>();
		variableDeclarations=new ArrayList<>();
		activities=new HashMap<>();
		transitions=new ArrayList<>();
	}

	/**
	 * get the transitions that are outgoing from 'source' 
	 *
	 * @param source
	 * @return a list of Transition elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getOutgoingTransitions(JSONObject source) throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject t: getTransitions()){
			if(t.getString("from").equals(source.getString("id")))res.add(t);
		}
		return res;
	}
	
	/**
	 * get the transitions that are incoming to 'sink'
	 *
	 * @param sink
	 * @return a list of Transition elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getIncomingTransitions(JSONObject sink) throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject t: getTransitions()){
			if(t.getString("to").equals(sink.getString("id")))res.add(t);
		}
		return res;
	}
	
	/**
	 * get the start activities for this workflow element,
	 * i.e. activities that are either of type "START"
	 * or have zero incoming transitions
	 * 
	 * @return a list of Activity elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getStartActivities() throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject a: getActivities()){
			if("START".equals(a.optString("type",null)) || getIncomingTransitions(a).size()==0){
				res.add(a);
				continue;
			}
		}
		return res;
	}
	
	/**
	 * get the start activities for this workflow element,
	 * i.e. activities that are either of type "START"
	 * or have zero incoming transitions
	 * 
	 * @return a list of Activity elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getStartSubflows() throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject a: getSubWorkflows()){
			if(getIncomingTransitions(a).size()==0){
				res.add(a);
				continue;
			}
		}
		return res;
	}
	
	/**
	 * get the end activities for this workflow element,
	 * i.e. activities that have zero incoming transitions 
	 * 
	 * @return a list of Activity elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getEndActivities() throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject a: getActivities()){
			if(getOutgoingTransitions(a).size()==0){
				res.add(a);
				continue;
			}
		}
		return res;
	}
	
	/**
	 * get the end subflows for this workflow element,
	 * i.e. sub workflows that have zero outgoing transitions
	 * 
	 * @return a list of Activity elements (not a live copy, but a fixed copy)
	 */
	public List<JSONObject>getEndSubflows() throws JSONException {
		List<JSONObject> res=new ArrayList<>();
		for(JSONObject a: getSubWorkflows()){
			if(getOutgoingTransitions(a).size()==0){
				res.add(a);
				continue;
			}
		}
		return res;
	}

	/**
	 * build a new WorkflowInfo from the supplied workflow definition
	 */
	public static WorkflowInfo build(JSONObject wf) throws JSONException {
		WorkflowInfo w=new WorkflowInfo();
		buildCommon(w, wf);
		return w;
	}
	
	/**
	 * build a new WorkflowInfo from the supplied subworkflow definition
	 */
	public static WorkflowInfo buildSubflow(JSONObject wf) throws JSONException {
		WorkflowInfo w=new WorkflowInfo();
		boolean isLoop = wf.optBoolean("is_loop_body", false);
		String iteratorName = wf.optString("iterator_name", null);
		w.setLoop(isLoop);
		w.setIteratorName(iteratorName);
		buildCommon(w, wf);
		return w;
	}
	
	
	private static void buildCommon(WorkflowInfo w, JSONObject wf) throws JSONException {
		List<JSONObject> swfs = getItemsWithID(wf, "subworkflows");
		for(JSONObject o: swfs){
			w.addSubWorkflow(o);
		}
		List<JSONObject> activities = getItemsWithID(wf, "activities");
		for(JSONObject o: activities){
			w.addActivity(o);
		}
		List<JSONObject> declarations = getItemsByKey(wf, "variables", "name");
		for(JSONObject o: declarations){
			w.addDeclaration(o);
		}
		JSONArray transitions = wf.optJSONArray("transitions");
		if(transitions!=null) {
			for(int i=0; i<transitions.length(); i++){
				w.addTransition(transitions.getJSONObject(i));
			}	
		}
	}
	private static List<JSONObject>getItemsWithID(JSONObject source, String name) throws JSONException {
		return getItemsByKey(source, name, "id");
	}
	
	public static List<JSONObject>getItemsByKey(JSONObject source, String name, String target) throws JSONException {
		List<JSONObject>result = new ArrayList<>();
		Object itemsDecl = source.opt(name);
		if(itemsDecl!=null && itemsDecl instanceof JSONArray) {
			JSONArray items = source.getJSONArray(name);
			for(int i=0; i<items.length(); i++){
				result.add(items.getJSONObject(i));	
			}
		}else if(itemsDecl!=null && itemsDecl instanceof JSONObject) {
			JSONObject items = source.getJSONObject(name);
			Iterator<?> iter = items.keys();
			while(iter.hasNext()) {
				String id = (String)iter.next();
				JSONObject item = items.getJSONObject(id);
				item.put(target, id);
				result.add(item);	
			}
		}
		return result;
	}
	
	public boolean isLoop() {
		return isLoop;
	}

	public void setLoop(boolean isLoop) {
		this.isLoop = isLoop;
	}

	public List<JSONObject> getSubWorkflows() {
		return new ArrayList<>(subWorkflows.values());
	}
	
	public List<JSONObject> getActivities() {
		return new ArrayList<>(activities.values());
	}

	public JSONObject getActivity(String id) {
		return activities.get(id);
	}

	public JSONObject getSubWorkflow(String id) {
		return subWorkflows.get(id);
	}

	public List<JSONObject> getActivities(String prefix) throws JSONException {
		List<JSONObject>result = new ArrayList<>();
		for(JSONObject a: getActivities()){
			if(a.getString("id").startsWith(prefix)){
				result.add(a);
			}
		}
		return result;
	}

	public List<JSONObject> getTransitions() {
		return transitions;
	}
	
	public JSONObject getTransition(String id) throws JSONException {
		for(JSONObject t: transitions){
			if(id.equals(t.getString("id")))return t;
		}
		return null;
	}

	public List<JSONObject> getDeclarations() {
		return variableDeclarations;
	}

	public boolean addDeclaration(JSONObject var){
		variableDeclarations.add(var);
		return true;
	}
	
	public boolean addTransition(JSONObject transition){
		transitions.add(transition);
		return true;
	}

	public boolean addTransition(String id1, String id2) throws JSONException {
		if(!entityExists(id1)){
			throw new IllegalArgumentException("No matching 'from' activity or subflow");
		}
		if(!entityExists(id2)){
			throw new IllegalArgumentException("No matching 'to' activity or subflow");
		}
		JSONObject tr = new JSONObject();
		tr.put("from", id1);
		tr.put("to", id2);
		tr.put("id", UUID.randomUUID().toString());
		addTransition(tr);
		return true;
	}

	public boolean addActivity(JSONObject activity) throws JSONException {
		activities.put(activity.getString("id"), activity);
		return true;
	}

	public boolean addSubWorkflow(JSONObject subWorkflow) throws JSONException {
		WorkflowInfo subW=build(subWorkflow);
		subWorkflowInfos.add(subW);
		String id=subWorkflow.getString("id");
		subWorkflows.put(id,subWorkflow);
		return true;
	}

	public String getIteratorName() {
		return iteratorName;
	}


	public void setIteratorName(String iteratorName) {
		this.iteratorName = iteratorName;
	}
	
	public boolean entityExists(String id){
		return getActivity(id)!=null || getSubWorkflow(id)!=null;
	}
	
}
