package eu.unicore.workflow.pe.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityContainer;

/**
 * During runtime of the workflow the {@link SubflowContainer} class is used to store 
 * information about a (sub)workflow.
 * 
 * @author schuller
 */
public class SubflowContainer implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String workflowID;
	
	private final Map<String,List<PEStatus>> activityStati=new HashMap<String,List<PEStatus>>();
	
	private List<String> outputFiles=new ArrayList<String>();

	//in case this is a subflow, this is the subflow ID
	private String id;

	//attributes for subflows
	private List<SubflowContainer> subFlowAttributes=new ArrayList<SubflowContainer>();
	
	private boolean isLoop=false;
	
	private List<String> parentIteratorNames=new ArrayList<String>();
	
	private String iteratorName;
	
	private String parentLoopID;
	
	private boolean isSplit=false;
	
	private String globalFatalError=null;
	
	private boolean isHeld=false;
	
	private Map<String,String>resumeParams;
	
	public SubflowContainer(){}

	public String getWorkflowID(){
		return workflowID;
	}
	
	public void setWorkflowID(String id){
		workflowID=id;
	}

	public List<String> getOutputFiles(){
		return outputFiles;
	}
	
	public void setOutputFiles(List<String>files){
		outputFiles=files;
	}

	public List<SubflowContainer> getSubFlowAttributes() {
		return subFlowAttributes;
	}

	public void setSubFlowAttributes(List<SubflowContainer> subFlowAttributes) {
		this.subFlowAttributes = subFlowAttributes;
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

	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * returns <code>true</code> if this SubflowContainer is a loop body
	 */
	public boolean isLoop(){
		return isLoop;
	}
	
	public void setIsLoop(boolean isLoop){
		this.isLoop=isLoop;
	}
	
	/**
	 * returns the ID of the parent loop, or <code>null</code> if no parent loop exists
	 */
	public String getParentLoopID(){
		return parentLoopID;
	}
	
	public void setParentLoopID(String parentLoopID){
		this.parentLoopID=parentLoopID;
	}
	
	/**
	 * returns the iterator variable name or <code>null</code> if this is not a loop
	 */
	public String getIteratorName(){
		return iteratorName;
	}

	/**
	 * sets the iterator name for this ActivityGroup
	 * 
	 * @param iteratorName
	 */
	public void setIteratorName(String iteratorName) {
		this.iteratorName = iteratorName;	
	}

	public List<String> getParentIteratorNames() {
		return parentIteratorNames;
	}

	public void setParentIteratorNames(List<String> parentIteratorNames) {
		this.parentIteratorNames = parentIteratorNames;
	}
	
	public void setupParentIteratorNames(List<String>parents){
		parentIteratorNames.addAll(parents);
		for(SubflowContainer a: subFlowAttributes){
			String iter=getIteratorName();
			List<String>sub=new ArrayList<String>();
			sub.addAll(parents);
			if(isLoop && iter!=null)sub.add(iter);
			a.setupParentIteratorNames(sub);
		}
		
	}

	public boolean isSplit() {
		return isSplit;
	}

	public void setSplit(boolean isSplit) {
		this.isSplit = isSplit;
	}

	public String getGlobalFatalError() {
		return globalFatalError;
	}

	public void setGlobalFatalError(String globalFatalError) {
		this.globalFatalError = globalFatalError;
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
			activityStati.put(act.getID(),new ArrayList<PEStatus>());
			this.setIsLoop(activityGroup.isLoop());
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
	 * recursively collect all job EPRs submitted for this sub-flow
	 * @return job EPRs
	 */
	public Collection<String> collectJobs() {
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

}
