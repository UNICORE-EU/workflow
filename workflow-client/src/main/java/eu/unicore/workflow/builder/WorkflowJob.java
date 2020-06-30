package eu.unicore.workflow.builder;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Job;

public class WorkflowJob extends Job {

	public WorkflowJob() {
	}

	public WorkflowJob(JSONObject json) {
		super(json);
	}

	public WorkflowJob ignore_failure() {
		JSONUtil.putQuietly(JSONUtil.getOrCreateObject(json, "options"), "IGNORE_FAILURE", "true");
		return this;
	}
	
	public WorkflowJob at_site(String name) {
		JSONUtil.putQuietly(json, "Site name", name);
		return this;
	}
}
