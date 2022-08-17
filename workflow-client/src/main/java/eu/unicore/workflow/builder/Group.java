package eu.unicore.workflow.builder;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;

public class Group {

	protected final JSONObject json;

	public Group(String id, JSONObject json) {
		this.json = json;
	}
	
	public Group(String id) {
		this(id, new JSONObject());
	}

	public JSONObject getJSON() {
		return json;
	}
	
	public Group subworkflow(String id) {
		Group g = new Group(id);
		JSONUtil.getOrCreateObject(json, "subworkflows").put(id, g.json);
		return g;
	}
	
	public WorkflowJob job(String id) {
		WorkflowJob j = new WorkflowJob();
		JSONUtil.putQuietly(j.getJSON(), "id", id);
		JSONUtil.getOrCreateObject(json, "activities").put(id, j.getJSON());
		return j;
	}
	
	public Group transition(String from, String to) {
		return transition(from, to, null);
	}

	public Group transition(String from, String to, String condition) {
		JSONObject tr = new JSONObject();
		JSONUtil.putQuietly(tr, "from", from);
		JSONUtil.putQuietly(tr, "to", to);
		JSONUtil.putQuietly(tr, "id", from+"-->"+to);
		if(condition!=null) {
			JSONUtil.putQuietly(tr, "condition", condition);
		}
		JSONUtil.getOrCreateArray(json, "transitions").put(tr);
		return this;
	}
	
	public Variable variable(String name) {
		Variable var = new Variable(name);
		JSONUtil.getOrCreateObject(json, "variables").put(name, var.getJSON());
		return var;
	}
	
	public VariableModification modify_variable(String id) {
		VariableModification var = new VariableModification(id);
		JSONUtil.getOrCreateObject(json, "activities").put(id, var.getJSON());
		return var;
	}
	
	public Group option(String key, String value) {
		JSONUtil.putQuietly(JSONUtil.getOrCreateObject(json, "options"), key, value);
		return this;
	}
	
	public Group cobroker() {
		return option("COBROKER", "true");
	}

	public ForEach for_each(String id) {
		ForEach fe = new ForEach(id);
		JSONUtil.getOrCreateObject(json, "subworkflows").put(id, fe.json);
		return fe;
	}
}
