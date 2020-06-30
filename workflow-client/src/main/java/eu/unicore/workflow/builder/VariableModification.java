package eu.unicore.workflow.builder;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;

public class VariableModification {

	protected final JSONObject json;

	public VariableModification(String id) {
		this.json = new JSONObject();
		JSONUtil.putQuietly(json, "id", id);
	}

	public JSONObject getJSON() {
		return json;
	}
	
	public VariableModification name(String val) {
		JSONUtil.putQuietly(json, "name", val);
		return this;
	}
	
	public VariableModification expression(String val) {
		JSONUtil.putQuietly(json, "expression", val);
		return this;
	}
	

}
