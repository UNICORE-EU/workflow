package eu.unicore.workflow.pe.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.persist.util.Wrapper;

/**
 * An abstract container for activities
 * 
 * @author schuller
 */
public abstract class ActivityContainer extends Activity{

	private static final long serialVersionUID=1;

	final Map<String,Wrapper<Activity>>activities = new HashMap<>();

	protected final Map<String,DeclareVariableActivity>declarations = new HashMap<>();

	protected String loopIteratorName=null;

	ActivityContainer(){}
	
	public ActivityContainer(String id,String workflowID) {
		super(id,workflowID);
	}

	public ActivityContainer(String id,String workflowID, Iterate iteration) {
		super(id,workflowID,iteration);
	}
	
	/**
	 * returns a copy of the list containing all activities
	 */
	public List<Activity> getActivities(){
		List<Activity>result=new ArrayList<Activity>();
		for(Wrapper<Activity> wa: activities.values()){
			result.add(wa.get());
		}
		return result;
	}

	public Activity getActivity(String id) {
		Wrapper<Activity>w = activities.get(id);
		return w != null? w.get() : null;
	}

	public void setActivities(List<Activity> activities) {
		this.activities.clear();
		for(Activity a: activities){
			this.activities.put(a.getID(), new Wrapper<>(a));
		}
	}

	public void setActivities(Activity... activities) {
		setActivities(Arrays.asList(activities));
	}

	public void setDeclarations(List<DeclareVariableActivity> declarations) {
		this.declarations.clear();
		for(DeclareVariableActivity a: declarations){
			this.declarations.put(a.getID(), a);
		}
	}

	public void setDeclarations(DeclareVariableActivity... declarations) {
		setDeclarations(Arrays.asList(declarations));
	}

	public Collection<DeclareVariableActivity> getDeclarations() {
		return declarations.values();
	}
	/**
	 * sets the variable name that is used as a loop iterator
	 * @param iteratorName
	 */
	public void setLoopIteratorName(String iteratorName){
		this.loopIteratorName=iteratorName;
	}

	public String getLoopIteratorName(){
		return loopIteratorName;
	}

	/**
	 * Checks if this is a loop. To make this a loop, call setLoopIteratorName() with
	 * a non-zero variable name
	 * 
	 * @return true if loop iterator name is not null
	 */
	public boolean isLoop(){
		return loopIteratorName!=null;
	}


	public ActivityContainer clone(){
		ActivityContainer cloned=(ActivityContainer)super.clone();

		//clone activities
		for(Map.Entry<String, Wrapper<Activity>> entry:this.activities.entrySet()){
			Activity clonedActivity=entry.getValue().get().clone();
			cloned.activities.put(entry.getKey(), new Wrapper<>(clonedActivity));
		}
		//clone declarations
		for(Map.Entry<String, DeclareVariableActivity> entry:this.declarations.entrySet()){
			DeclareVariableActivity clonedActivity=(DeclareVariableActivity)entry.getValue().clone();
			cloned.declarations.put(entry.getKey(), clonedActivity);
		}
		return cloned;
	}

}
