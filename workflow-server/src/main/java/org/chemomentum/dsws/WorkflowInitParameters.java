package org.chemomentum.dsws;

import java.util.Calendar;

import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.workflow.pe.files.Locations;

public class WorkflowInitParameters extends BaseInitParameters {

	public WorkflowInitParameters(String uuid, Calendar terminationTime) {
		super(uuid,	terminationTime!=null? null: TerminationMode.DEFAULT, 
				terminationTime);
	}
	
	public String storageURL;
	
	public String workflowName;

	public String[] initialTags;
	
	public ConversionResult cr;

	public Locations locations;
}
