package eu.unicore.workflow.builder;

import org.json.JSONObject;

import eu.unicore.uas.json.JSONUtil;

public class VariableModification {

	protected final JSONObject json;
	protected final String id;

	public VariableModification(String id) {
		this.json = new JSONObject();
		this.id = id;
	}

	public String getID() {
		return id;
	}
	
	public JSONObject getJSON() {
		return json;
	}
	
	public VariableModification name(String val) {
		JSONUtil.putQuietly(json, "variable_name", val);
		return this;
	}
	
	public VariableModification expression(String val) {
		JSONUtil.putQuietly(json, "expression", val);
		return this;
	}
	

}
