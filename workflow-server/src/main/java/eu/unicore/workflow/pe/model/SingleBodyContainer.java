package eu.unicore.workflow.pe.model;

import java.util.List;

import eu.unicore.persist.util.Wrapper;

public abstract class SingleBodyContainer extends ActivityContainer {
	
	private static final long serialVersionUID=1;
	
	private Wrapper<ActivityGroup> body;

	public SingleBodyContainer(String id, String workflowID, ActivityGroup body) {
		super(id, workflowID);
		this.body=body!=null?new Wrapper<>(body):null;
		super.setActivities(body);
	}

	public SingleBodyContainer(String id, String workflowID, Iterate iteration, ActivityGroup body) {
		super(id, workflowID, iteration);
		this.body=new Wrapper<>(body);
		super.setActivities(body);
	}
	
	public ActivityGroup getBody() {
		return body!=null?body.get():null;
	}
	
	@Override
	public void setActivities(Activity... activities) {
		if(activities.length!=1)throw new IllegalArgumentException("For-each supports only a single activity");
		if(!(activities[0] instanceof ActivityGroup)) {
			throw new IllegalArgumentException("For-each body must be a group");
		}
		super.setActivities(activities);
		body=new Wrapper<>((ActivityGroup)activities[0]);
	}

	@Override
	public void setActivities(List<Activity> activities) {
		if(activities.size()!=1)throw new IllegalArgumentException("For-each supports only a single activity");
		super.setActivities(activities);
		if(!(activities.get(0) instanceof ActivityGroup)) {
			throw new IllegalArgumentException("For-each body must be a group");
		}
		body=new Wrapper<>((ActivityGroup)activities.get(0));
	}

	public SingleBodyContainer clone(){
		SingleBodyContainer cloned=(SingleBodyContainer)super.clone();
		return cloned;
	}
}
