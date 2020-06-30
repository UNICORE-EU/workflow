package eu.unicore.workflow.builder;

import org.json.JSONObject;

public class Workflow extends Group {

	public Workflow(JSONObject json) {
		super(null, json);
	}
	
	public Workflow() {
		super(null);
	}

	public final JSONObject getJSON() {
		return json;
	}

}
