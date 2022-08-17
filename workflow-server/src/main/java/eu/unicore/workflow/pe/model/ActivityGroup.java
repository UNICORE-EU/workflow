package eu.unicore.workflow.pe.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.fzj.unicore.persist.util.Wrapper;
import de.fzj.unicore.xnjs.ems.ProcessingException;

import eu.unicore.util.Log;
import eu.unicore.workflow.pe.iterators.IterationException;
import eu.unicore.workflow.pe.xnjs.Constants;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * A group of activities connected by transitions
 * 
 * @author schuller
 */
public class ActivityGroup extends ActivityContainer{
	
	private static final long serialVersionUID=1;

	public static final String ACTION_TYPE="UNICORE_WORKFLOW";
	
	public static final String OPTION_COBROKER="COBROKER";
	
	private List<Transition>transitions = new ArrayList<>();
	
	private boolean coBrokerActivities=false;
	
	private String notificationURL = null;
	
	public ActivityGroup(){}
	
	public ActivityGroup(String id,String workflowID) {
		super(id,workflowID);
	}
	
	public ActivityGroup(String id,String workflowID, Iterate iteration) {
		super(id,workflowID,iteration);
	}
	
	/**
	 * get all activities that can be submitted
	 * 
	 * @return non-null list of Activity objects in state READY
	 */
	public List<Activity> getDueActivities(){
		List<Activity>result = new ArrayList<>();
		for(Wrapper<Activity> wa: activities.values()){
			if(ActivityStatus.READY==wa.get().getStatus())result.add(wa.get());
		}
		return result;
	}
	
	public List<Transition> getTransitions(){
		return transitions;
	}
	
	public void setTransitions(Transition... transitions) {
		this.transitions = Arrays.asList(transitions);
	}

	public void addTransition(Transition t) {
		transitions.add(t);
	}

	public boolean removeTransition(Transition t) {
		Iterator<Transition>iter=transitions.iterator();
		while(iter.hasNext()){
			if(iter.next().getID().equals(t.getID())){
				iter.remove();
				return true;
			}
		}
		return false;
	}

	public void init()throws ProcessingException{
		init(null);
	}

	/**
	 * update this group: put activities that can be submitted 
	 * into "READY" state.
	 */
	public void init(final ProcessVariables vars)throws ProcessingException{
		for(Wrapper<Activity> wa: activities.values()){
			Activity a = wa.get();
			try{
				a.getIterate().reset(vars);
			}catch(IterationException ex){
				throw new ProcessingException(ex);
			}
			if(isStartActivity(a)){
					a.status=ActivityStatus.READY;
			}
			else a.status=ActivityStatus.CREATED;
		}
	}
	
	/**
	 * put all eligible follow-on activities into "READY" state
	 */
	public void activityDone(Activity a, ProcessVariables vars)throws EvaluationException{
		//find outgoing transitions
		List<Transition>out=findOutgoingTransitions(a);
		boolean followFirst=SplitType.FOLLOW_FIRST_MATCHING.equals(a.splitType);
		for(Transition t: out){
			//now find activity and set to READY *IF* all the incoming 
			//transitions are from finished activities
			Activity target=getActivity(t.to);
			if(target==null)throw new IllegalStateException("Target activity <"+t.to+"> does not exist.");
			if(!checkDependencies(target, a)){
				//process next transition
				continue;
			}
		
			if(t.isConditional()){
				Condition cond=t.getCondition();
				cond.setProcessVariables(vars);
				String currentIteration=vars.get(Constants.PV_KEY_ITERATION,String.class);
				cond.setIterationValue(currentIteration);
				if(!cond.evaluate()){
					//process next transition
					continue;
				}
			}
			target.status=ActivityStatus.READY;
			if(followFirst)break;
		}
	}

	@Override
	public String getType(){
		return ACTION_TYPE;
	}
	
	public boolean isStartActivity(Activity a){
		String id=a.id;
		for(Transition t: transitions){
			if(id.equals(t.to))return false;
		}
		return true;
	}
	
	public List<Transition>findOutgoingTransitions(Activity a){
		List<Transition>result = new ArrayList<>();
		String id=a.id;
		for(Transition t: transitions){
			if(id.equals(t.from))result.add(t);
		}
		return result;
	}

	/**
	 * get all incoming transitions
	 * 
	 * @param a - the activity to check
	 */
	public List<Transition>findIncomingTransitions(Activity a){
		List<Transition>result = new ArrayList<>();
		String id=a.id;
		for(Transition t: transitions){
			if(id.equals(t.to))result.add(t);
		}
		return result;
	}

	/**
	 * check that all activities that have a transition to the given activity 
	 * have status SUCCESS
	 * 
	 * @param a -  the Activity to check
	 * @param excludedDep - a predecessor that need not be checked (can be <code>null</code>)
	 * @return true if all dependencies (i.e. "incoming" activities) are successful
	 */
	public boolean checkDependencies(Activity a, Activity excludedDep){
		//do not check if type is MERGE
		if(MergeType.MERGE.equals(a.getMergeType()))return true;
		List<Transition>tr=findIncomingTransitions(a);
		for(Transition t: tr){
			Activity from=getActivity(t.from);
			if(excludedDep!=null && from.id.equals(excludedDep.id))continue;
			ActivityStatus dep=from.status;
			if(ActivityStatus.SUCCESS!=dep)return false;
		}
		return true;
	}
	
	
	public boolean isCoBrokerActivities() {
		return coBrokerActivities;
	}

	public void setCoBrokerActivities(boolean coBrokerActivities) {
		this.coBrokerActivities = coBrokerActivities;
	}

	public ActivityGroup clone(){
		try{
			ActivityGroup cloned=(ActivityGroup)super.clone();
			List<Transition>clonedTr=new ArrayList<Transition>();
			//clone transitions
			for(Transition entry:this.transitions){
				Transition clonedTransition=entry.clone();
				clonedTr.add(clonedTransition);
			}
			cloned.transitions=clonedTr;
			return cloned;
		}
		catch(CloneNotSupportedException ce){
			Log.logException("Clone of "+this.getClass().getName()+" not supported", ce);
			return this;
		}
	}
	
	
	public void setNotificationURL(String notificationURL) {
		this.notificationURL = notificationURL;
	}
	
	public String getNotificationURL(){
		return notificationURL;
	}
	
}
