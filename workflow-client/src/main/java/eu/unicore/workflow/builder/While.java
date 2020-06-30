package eu.unicore.workflow.builder;

import de.fzj.unicore.uas.json.JSONUtil;

public class While extends Loop {

	public While(String id) {
		super(id);
		JSONUtil.putQuietly(json, "type", String.valueOf(LoopType.WHILE));
	}
	
	public While condition(String expr) {
		JSONUtil.putQuietly(json, "condition", expr);
		return this;
	}

	public While iterator_name(String name) {
		super.iterator_name(name);
		return this;
	}

}
