package eu.unicore.workflow;

import org.json.JSONObject;

public class Workflow {

	
	protected JSONObject json;

	public Workflow(JSONObject json) {
		this.json = json;
	}

	public Workflow() {
		this(new JSONObject());
	}

	public final JSONObject getJSON() {
		return json;
	}

}
