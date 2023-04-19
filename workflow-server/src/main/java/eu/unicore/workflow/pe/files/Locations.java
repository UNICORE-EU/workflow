package eu.unicore.workflow.pe.files;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.annotations.Table;
import eu.unicore.persist.util.JSON;
import eu.unicore.persist.util.Wrapper;


/**
 * store names / physical locations of files for a workflow
 * 
 * @author schuller
 */
@Table(name="WORKFLOWFILES")
@JSON(customHandlers={Wrapper.WrapperConverter.class})
public class Locations implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private String workflowID;
	
	private final Map<String,String> fileLocations = new HashMap<>();
	
	@ID
	public String getWorkflowID(){
		return workflowID;
	}

	public void setWorkflowID(String wfID) {
		this.workflowID = wfID;
	}
	
	public Map<String,String> getLocations() {
		return fileLocations;
	}
	
}
