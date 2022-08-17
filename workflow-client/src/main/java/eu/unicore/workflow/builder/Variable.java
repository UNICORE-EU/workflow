package eu.unicore.workflow.builder;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;

public class Variable {

	protected final JSONObject json;
	protected final String name;
	/**
	 * declare a new variable (defaults to type INTEGER value '0')
	 * 
	 * @param name
	 */
	public Variable(String name) {
		this.json = new JSONObject();
		this.name = name;
		JSONUtil.putQuietly(json, "type", "INTEGER");
		JSONUtil.putQuietly(json, "initial_value", "0");
	}

	public JSONObject getJSON() {
		return json;
	}
	
	public String getName() {
		return name;
	}
	
	public Variable initial_value(String val) {
		JSONUtil.putQuietly(json, "initial_value", val);
		return this;
	}
	
	public Variable integer() {
		JSONUtil.putQuietly(json, "type", "INTEGER");
		return this;
	}
	
	public Variable bool() {
		JSONUtil.putQuietly(json, "type", "BOOLEAN");
		return this;
	}
	
	public Variable string() {
		JSONUtil.putQuietly(json, "type", "STRING");
		return this;
	}
	
	public Variable as_float() {
		JSONUtil.putQuietly(json, "type", "FLOAT");
		return this;
	}
}
