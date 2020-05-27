package eu.unicore.workflow.pe.model;

import java.util.List;

import de.fzj.unicore.persist.util.Wrapper;

public abstract class SingleBodyContainer extends ActivityContainer {
	
	private static final long serialVersionUID=1;
	
	private Wrapper<Activity> body;

	public SingleBodyContainer(String id, String workflowID, Activity body) {
		super(id, workflowID);
		this.body=body!=null?new Wrapper<Activity>(body):null;
		super.setActivities(body);
	}

	public SingleBodyContainer(String id, String workflowID, Iterate iteration, Activity body) {
		super(id, workflowID, iteration);
		this.body=new Wrapper<Activity>(body);
		super.setActivities(body);
	}
	
	public Activity getBody() {
		return body!=null?body.get():null;
	}
	
	@Override
	public void setActivities(Activity... activities) {
		if(activities.length!=1)throw new IllegalArgumentException("For-each supports only a single activity");
		super.setActivities(activities);
		body=new Wrapper<Activity>(activities[0]);
	}

	@Override
	public void setActivities(List<Activity> activities) {
		if(activities.size()!=1)throw new IllegalArgumentException("For-each supports only a single activity");
		super.setActivities(activities);
		body=new Wrapper<Activity>(activities.get(0));
	}

	public SingleBodyContainer clone(){
		SingleBodyContainer cloned=(SingleBodyContainer)super.clone();
		return cloned;
	}
}
