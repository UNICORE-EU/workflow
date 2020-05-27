package eu.unicore.workflow.pe.xnjs;

import java.io.Serializable;

/**
 * store usage information about a workflow 
 * @author schuller
 */
public class Statistics implements Serializable{

	private static final long serialVersionUID = 1L;

	private final long startTime;
	
	// number of jobs
	private int totalJobs;
	
	public Statistics(){
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * get the total runtime in seconds
	 */
	public String getTotalRuntime(){
		return String.valueOf((int)(System.currentTimeMillis()-startTime)/1000);
	}
	
	public void incrementJobs(){
		totalJobs++;
	}
	
	public int getTotalJobs(){
		return totalJobs;
	}
	
	public void addAll(Statistics other){
		if(other == null)return;
		
		totalJobs+=other.totalJobs;
	}

}
