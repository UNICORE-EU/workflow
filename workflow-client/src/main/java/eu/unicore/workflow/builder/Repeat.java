package eu.unicore.workflow.builder;

import de.fzj.unicore.uas.json.JSONUtil;

public class Repeat extends Loop {

	public Repeat(String id) {
		super(id);
		JSONUtil.putQuietly(json, "type", String.valueOf(LoopType.REPEAT_UNTIL));
	}
	
	public Repeat condition(String expr) {
		JSONUtil.putQuietly(json, "condition", expr);
		return this;
	}

	public Repeat iterator_name(String name) {
		super.iterator_name(name);
		return this;
	}

}
