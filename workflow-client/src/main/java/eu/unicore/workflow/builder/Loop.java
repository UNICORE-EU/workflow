package eu.unicore.workflow.builder;

import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.uas.json.JSONUtil;

public abstract class Loop {

	public static enum LoopType {
		GROUP, FOR_EACH, WHILE, REPEAT_UNTIL
	}
	
	protected final JSONObject json;

	protected final String id;

	public Loop(String id) {
		this.json = new JSONObject();
		this.id=id;
	}
	
	public JSONObject getJSON() {
		return json;
	}

	public String getID() {
		return id;
	}
	
	public Loop type(LoopType type) {
		JSONUtil.putQuietly(json, "type", String.valueOf(type));
		return this;
	}
	
	public Group body() {
		Group b = null;
		JSONObject body = json.optJSONObject("body");
		String bid = id+"__body__";
		if(body!=null) {
			try{
				b = new Group(bid , body);
			}catch(JSONException e) {
				throw new IllegalStateException(e);
			}
		}
		else {
			b = new Group(bid);
			JSONUtil.putQuietly(json, "body", b.getJSON());
		}
		return b;
	}

	public Loop iterator_name(String name) {
		JSONUtil.putQuietly(json, "iterator_name", name);
		return this;
	}
}
