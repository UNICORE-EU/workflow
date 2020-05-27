package org.chemomentum.dsws;

import java.util.Calendar;

import de.fzj.unicore.uas.impl.BaseInitParameters;

public class WorkflowInitParameters extends BaseInitParameters {

	public WorkflowInitParameters(String uuid, Calendar terminationTime) {
		super(uuid,	terminationTime!=null? null: TerminationMode.DEFAULT, 
				terminationTime);
	}
	
	public String storageURL;
	
	public String workflowName;

}
