/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
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
 * Holds information about a simpleworkflow instance, used during conversion 
 * 
 * @author schuller
 */
public class WorkflowInfo {

	private List<WorkflowInfo> subWorkflowInfos;
	
	private boolean isRoot=true;
	
	private WorkflowInfo parent;
	
	protected String id;
	protected java.util.Map<String,JSONObject> subWorkflows;
	protected List<JSONObject> variableDeclarations;
	protected Map<String,JSONObject> activities;
	protected List<JSONObject> transitions;
	protected final Map<String,String> options = new HashMap<>();
	
	private boolean isLoop=false;
	private String iteratorName;
	
	public WorkflowInfo(){
		subWorkflowInfos=new ArrayList<WorkflowInfo>();
		subWorkflows=new HashMap<>();
		variableDeclarations=new ArrayList<>();
		activities=new HashMap<>();
		transitions=new ArrayList<>();
	}


	/**
	 * check hierarchically for an option value,
	 * Order of checking:
	 * <ul>
	 * <li>Activity options</li>
	 * <li>options defined in this (sub)workflow</li>
	 * <li>options defined in the parent, if any</li>
	 * </ul>
	 */
	public String getContextVariable(String key, String activityID, String defaultValue){
		String res=null;
		
		JSONObject a = getActivity(activityID);
		if(a!=null){
			JSONObject opts = a.optJSONObject("options");
			res = opts.optString(key, null);
		}
		
		if(res==null){
			res = getOptionValue(key,defaultValue);
		}
		
		if(res==null && parent!=null){
			res = parent.getContextVariable(key,activityID,defaultValue);
		}
		return res;
	}
	
	/**
	 * get the transitions that are outgoing from the source 
	 * 
	 * @param a - The activity
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
	 * get the transitions that are incoming to Subworkflow sub
	 * 
	 * @param sub - The activity
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
	 * checks whether the given activity is a Start activity, i.e. is of type START, or
	 * has no incoming transitions
	 */
	public boolean isStartActivity(JSONObject a) throws JSONException {
		return ("START".equals(a.getString("type")) 
				|| getIncomingTransitions(a).size()==0);
	}
	
	/**
	 * get the (sub)workflow containing the given activity
	 * @param activityID -  the id of the activity to search for
	 * @return WorkflowInfo for the parent container
	 */
	public WorkflowInfo getContainer(String activityID){
		if(getActivity(activityID)!=null)return this;
		for(WorkflowInfo sub: subWorkflowInfos){
			if(sub.getActivity(activityID)!=null)return this;
		}
		return null;
	}
	
	/**
	 * build a new WorkflowInfo from the supplied workflow definition
	 */
	public static WorkflowInfo build(JSONObject wf) throws JSONException {
		WorkflowInfo w=new WorkflowInfo();
		w.isRoot=true;
	
		buildCommon(w, wf);
		
		return w;
	}
	
	/**
	 * build a new WorkflowInfo from the supplied subworkflow definition
	 */
	public static WorkflowInfo buildSubflow(JSONObject wf) throws JSONException {
		WorkflowInfo w=new WorkflowInfo();
		w.setId(wf.getString("id"));
		boolean isLoop = wf.optBoolean("is_loop_body", false);
		String iteratorName = wf.optString("iterator_name", null);
		w.setLoop(isLoop);
		w.setIteratorName(iteratorName);
		w.isRoot=false;
		
		buildCommon(w, wf);
		
		return w;
	}
	
	
	private static void buildCommon(WorkflowInfo w, JSONObject wf) throws JSONException {
		JSONArray swfs = wf.optJSONArray("subworkflows");
		if(swfs!=null) {
			for(int i=0; i<swfs.length(); i++){
				w.addSubWorkflow(swfs.getJSONObject(i));
			}	
		}
		JSONArray activities = wf.optJSONArray("activities");
		if(activities!=null) {
			for(int i=0; i<activities.length(); i++){
				w.addActivity(activities.getJSONObject(i));
			}	
		}
		JSONArray declarations = wf.optJSONArray("variables");
		if(declarations!=null) {
			for(int i=0; i<declarations.length(); i++){
				w.addDeclaration(declarations.getJSONObject(i));
			}	
		}
		JSONArray transitions = wf.optJSONArray("transitions");
		if(transitions!=null) {
			for(int i=0; i<transitions.length(); i++){
				w.addTransition(transitions.getJSONObject(i));
			}	
		}
	}
	
	public boolean isLoop() {
		return isLoop;
	}

	public void setLoop(boolean isLoop) {
		this.isLoop = isLoop;
	}
	
	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
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

	public void addOption(String key, String value){
		options.put(key,value);
	}
	
	public Map<String,String> getOptions() {
		return options;
	}

	public String getOptionValue(String key) {
		return getOptionValue(key,null);
	}

	public String getOptionValue(String key, String defaultValue) {
		return options.getOrDefault(key, defaultValue);
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
	
	public boolean removeTransition(JSONObject transition) throws JSONException {
		Iterator<JSONObject>iterator=transitions.iterator();
		while(iterator.hasNext()){
			if(iterator.next().getString("id").equals(transition.getString("id"))){
				iterator.remove();
				return true;
			}
		}
		return false;
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
	
	public void removeActivity(JSONObject activity) throws JSONException {
		activities.remove(activity.getString("id"));
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean addSubWorkflow(JSONObject subWorkflow) throws JSONException {
		WorkflowInfo subW=build(subWorkflow);
		subW.isRoot=false;
		subW.parent=this;
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
