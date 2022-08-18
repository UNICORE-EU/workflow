package eu.unicore.workflow.pe.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityContainer;
import eu.unicore.workflow.pe.model.ActivityStatus;

/**
 * During runtime of the workflow the {@link SubflowContainer} class is used to store 
 * information about a (sub)workflow.
 * 
 * @author schuller
 */
public class SubflowContainer implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String workflowID;
	
	private final Map<String,List<PEStatus>> activityStati = new HashMap<>();

	//in case this is a subflow, this is the subflow ID
	private String id;

	//attributes for subflows
	private final List<SubflowContainer> subFlowAttributes = new ArrayList<>();

	private boolean isHeld=false;
	
	private Map<String,String>resumeParams;
	
	public SubflowContainer(){}

	public String getWorkflowID(){
		return workflowID;
	}

	public List<SubflowContainer> getSubFlowAttributes() {
		return subFlowAttributes;
	}

	/**
	 * recursively find the attributes belonging to a given subflow
	 */
	public SubflowContainer findSubFlowAttributes(String subflowID){
		if(subflowID.equals(getId()))return this;
		else{
			//first look top-level, which will probably be the usual access pattern
			for(SubflowContainer attr: subFlowAttributes){
				if(id.equals(attr.id))return attr;
			}
			//otherwise go down into the subflows
			for(SubflowContainer attr: subFlowAttributes){
				SubflowContainer res=attr.findSubFlowAttributes(subflowID);
				if(res!=null)return res;
			}
		}
		return null;
	}
	
	/**
	 * recursively find the attributes containing a certain activity, or null if
	 * activity is not found
	 */
	public SubflowContainer findSubFlowContainingActivity(String activityID){
		if(activityStati.containsKey(activityID))return this;
		else{
			for(SubflowContainer subAttributes: subFlowAttributes){
				SubflowContainer res=subAttributes.findSubFlowContainingActivity(activityID);
				if(res!=null)return res;
			}
		}
		return null;
	}
	
	/**
	 * get the list of activity IDs for this (sub)flow
	 * (does not recurse into subflows)
	 */
	public Collection<String> getActivities() {
		return activityStati.keySet();
	}

	/**
	 * checks if the given activity is a subflow at top-level
	 *  
	 * @param activityId
	 */
	public boolean isSubFlow(String activityId){
		for(SubflowContainer wfc: getSubFlowAttributes()){
			if(wfc.getId().equals(activityId))return true;
		} 
		return false;
	}
	/**
	 * gets persistent information about the given activity. will not recurse into 
	 * sub-flows. Returns null if the activity does not exist. 
	 * 
	 * @param activityID
	 */
	public List<PEStatus> getActivityStatus(String activityID) {
		return getActivityStatus(activityID, false);
	}

	/**
	 * gets persistent information about the given activity, optionally recursing
	 * into sub-flows. Returns null if the activity does not exist. 
	 * 
	 * @param activityID - the activity ID
	 * @param recurse - whether to recurse into sub-flows
	 */
	public List<PEStatus> getActivityStatus(String activityID, boolean recurse) {
		List<PEStatus>res=activityStati.get(activityID);
		if(res==null && recurse){
			for(SubflowContainer subflow: subFlowAttributes){
				res=subflow.getActivityStatus(activityID, recurse);
				if(res!=null)return res;
			}
		}
		return res;
	}

	/**
	 * @param activityID
	 * @param iterationValue
	 */
	public PEStatus getActivityStatus(String activityID, String iterationValue) {
		List<PEStatus>res=activityStati.get(activityID);
		for(PEStatus p: res){
			if(p.getIteration().equals(iterationValue))return p;
		}
		throw new IllegalArgumentException("No such iteration <"+iterationValue+"> for activity <"+activityID+">");
	}
	
	/**
	 * gets a filtered list of stati for a certain activity
	 */
	public List<PEStatus> getActivityStatus(String activityID, Filter filter) {
		List<PEStatus>all=activityStati.get(activityID);
		List<PEStatus>res=new ArrayList<PEStatus>();
		for(PEStatus p: all){
			if(filter.accept(p.getIteration()))res.add(p);
		}
		return res;
	}
	
	public String getId() {
		return id;
	}

	/**
	 * parse the given ActivityGroup and setup this SubflowContainer instance
	 * 
	 * @param activityGroup
	 */
	public void build(ActivityContainer activityGroup){
		workflowID=activityGroup.getWorkflowID();
		id=activityGroup.getID();
		for(Activity act: activityGroup.getActivities()){
			if(act instanceof ActivityContainer){
				SubflowContainer subAttributes=new SubflowContainer();
				subAttributes.build((ActivityContainer)act);
				subFlowAttributes.add(subAttributes);
			}
			activityStati.put(act.getID(), new ArrayList<>());
		}
	}
	
	public boolean isHeld(){
		return isHeld;
	}
	
	public void hold(){
		isHeld=true;
	}
	
	public void resume(Map<String,String>params){
		this.resumeParams = params;
		isHeld=false;
	}
	
	public Map<String,String>getResumeParameters(){
		return resumeParams;
	}
	
	/**
	 * recursively collect all job URLs submitted for this sub-flow
	 * @return job URLs
	 */
	public List<String> collectJobs() {
		List<String>jobs = new ArrayList<>();
		for(List<PEStatus> as: activityStati.values()){
			for(PEStatus s: as){
				if(s.getJobURL()!=null) {
					jobs.add(s.getJobURL());
				}
			}
		}
		for(SubflowContainer sfc: subFlowAttributes){
			jobs.addAll(sfc.collectJobs());
		}
		return jobs;
	}
	
	/**
	 * get number of stored PEStatus elements (recurse into subflows)
	 */
	public long getSize() {
		long size = getSizeNoRecurse();
		for(SubflowContainer sc: subFlowAttributes) {
			size += sc.getSize();
		}
		return size;
	}
	
	private long getSizeNoRecurse() {
		long size = 0;
		for(List<PEStatus> as: activityStati.values()) {
			size += as.size();
		}
		return size;
	}

	/**
	 * reduce size of stored information
	 */
	public void compact() {
		compact(500);
	}

	public void compact(long sizeHint) {
		if(getSizeNoRecurse()>sizeHint) {
			for(List<PEStatus> as: activityStati.values()) {
				Iterator<PEStatus> iter = as.iterator();
				while(iter.hasNext()) {
					PEStatus s = iter.next();
					if(ActivityStatus.RUNNING!=s.getActivityStatus()) {
						iter.remove();
					}
				}
			}
		}
		for(SubflowContainer sc: subFlowAttributes) {
			sc.compact(sizeHint);
		}
	}
}
