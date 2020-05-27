package org.chemomentum.dsws;

import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.uas.impl.UASBaseModel;

public class WorkflowFactoryModel extends UASBaseModel {
	
	private static final long serialVersionUID = 1L;
	
	Map<String, String>owners=new HashMap<String, String>();

	String workflowEnumerationID;

	public Map<String, String> getOwners() {
		return owners;
	}

}
