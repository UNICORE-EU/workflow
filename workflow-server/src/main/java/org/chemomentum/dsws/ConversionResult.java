package org.chemomentum.dsws;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.workflow.pe.model.PEWorkflow;

/**
 * When submitting a workflow, the {@link DSLDelegate} produces
 * a set of attributes during the conversion process. 
 * These include conversion errors and the converted workflow 
 * 
 * @author schuller
 */
public class ConversionResult implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String workflowID;

	private String dialect;
	
	private List<String> outputFiles=new ArrayList<String>();
	
	private Map<String,String> declaredVariables=new HashMap<String, String>();
	
	private transient List<String> errors=new ArrayList<String>();
	
	private PEWorkflow convertedWorkflow;
	
	public ConversionResult(){}
	
	/**
	 * check whether errors occured during conversion of the domain workflow
	 */
	public boolean hasConversionErrors(){
		return errors.size()>0;
	}
	
	/**
	 * get the conversion errors produced while converting the domain workflow 
	 */
	public List<String> getConversionErrors(){
		return errors;
	}
	
	public void addError(String error){
		errors.add(error);
	}
	
	public String getDialect(){
		return dialect;
	}
	
	public void setDialect(String dialect){
		this.dialect=dialect;
	}

	public String getWorkflowID(){
		return workflowID;
	}
	
	public void setWorkflowID(String id){
		workflowID=id;
	}

	public List<String> getOutputFiles(){
		return outputFiles;
	}
	
	public void setOutputFiles(List<String>files){
		outputFiles=files;
	}

	public PEWorkflow getConvertedWorkflow() {
		return convertedWorkflow;
	}

	public void setConvertedWorkflow(PEWorkflow convertedWorkflow) {
		this.convertedWorkflow = convertedWorkflow;
	}

	public Map<String, String> getDeclaredVariables() {
		return declaredVariables;
	}

}
