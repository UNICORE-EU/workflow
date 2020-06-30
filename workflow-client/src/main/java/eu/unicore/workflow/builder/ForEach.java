package eu.unicore.workflow.builder;

import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;

public class ForEach extends Loop {

	public ForEach(String id) {
		super(id);
		JSONUtil.putQuietly(json, "type", String.valueOf(LoopType.FOR_EACH));
	}

	public ForEach iterator_name(String name) {
		super.iterator_name(name);
		return this;
	}
	
	public ForEach values(String... values) {
		JSONArray array = new JSONArray();
		if(values!=null){
			for(String s: values) {
					array.put(s);
			}
		}
		JSONUtil.putQuietly(json, "values", array);
		return this;
	}

	public ForEach.VariableIterator variable() {
		VariableIterator vi = new VariableIterator();
		JSONArray vars = JSONUtil.getOrCreateArray(json, "variables");
		vars.put(vi.json);
		return vi;
	}

	public ForEach.FilesetIterator fileset() {
		FilesetIterator fi = new FilesetIterator();
		JSONArray vars = JSONUtil.getOrCreateArray(json, "filesets");
		vars.put(fi.json);
		return fi;
	}
	
	public static class VariableIterator {
		protected JSONObject json = new JSONObject();
		
		public VariableIterator named(String name) {
			JSONUtil.putQuietly(json, "variable_name", name);
			return this;
		}
		
		public VariableIterator type(String type) {
			JSONUtil.putQuietly(json, "type", type);
			return this;
		}
		
		public VariableIterator start_value(String val) {
			JSONUtil.putQuietly(json, "start_value", val);
			return this;
		}
		
		public VariableIterator end_condition(String c) {
			JSONUtil.putQuietly(json, "end_condition", c);
			return this;
		}
		
		public VariableIterator expression(String expr) {
			JSONUtil.putQuietly(json, "expression", expr);
			return this;
		}
	}
	
	public static class FilesetIterator {
		protected JSONObject json = new JSONObject();
		
		public FilesetIterator base_url(String base_url) {
			JSONUtil.putQuietly(json, "base", base_url);
			return this;
		}
		
		public FilesetIterator includes(String... includes) {
			JSONArray array = new JSONArray();
			if(includes!=null){
				for(String s: includes) {
						array.put(s);
				}
			}
			JSONUtil.putQuietly(json, "includes", array);
			return this;
		}
		
		public FilesetIterator excludes(String... excludes) {
			JSONArray array = new JSONArray();
			if(excludes!=null){
				for(String s: excludes) {
						array.put(s);
				}
			}
			JSONUtil.putQuietly(json, "excludes", array);
			return this;
		}
		
		public FilesetIterator recurse() {
			JSONUtil.putQuietly(json, "recurse", "true");
			return this;
		}
		
		public JSONObject getJSON() {
			return json;
		}
	}
}
