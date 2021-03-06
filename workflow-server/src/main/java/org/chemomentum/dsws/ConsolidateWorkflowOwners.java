package org.chemomentum.dsws;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.util.Log;


/**
 * after server restart, this class fixes any inconsistencies in the list of
 * workflow IDs and workflow owners
 * 
 * @author schuller
 */
public class ConsolidateWorkflowOwners implements Runnable {

	private static final Logger logger=Log.getLogger(Log.SERVICES,ConsolidateWorkflowOwners.class);

	private final String workflowFactoryID;
	private final Kernel kernel;
	private final Map<String,String>owners;
	
	public ConsolidateWorkflowOwners(String workflowFactoryID, Map<String,String>owners, Kernel kernel){
		this.workflowFactoryID=workflowFactoryID;
		this.kernel=kernel;
		this.owners=new HashMap<String, String>();
		owners.putAll(owners);
	}
	
	public void run() {
		Home h=kernel.getHome(WorkflowHome.SERVICE_NAME);
		Set<String>uids=new HashSet<String>();
		try{
			uids.addAll(h.getStore().getUniqueIDs());
			for(String id: uids){
				try{
					if(owners.containsKey(id))continue;
					
					ResourceImpl res=(ResourceImpl)h.get(id);
					if(res.getOwner()!=null){
						owners.put(id,res.getOwner());
					}
				}
				catch(Exception ex){
					Log.logException("Error getting owner for workflow <"+id+">", ex, logger);
				}
			}
			storeChanges();
		}
		catch(Exception ex){
			logger.error("Error updating workflow owners for factory instance <"+workflowFactoryID+">",ex);
		}
	}
	
	private void storeChanges()throws Exception{
		Home h=kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		WorkflowFactoryImpl wf=(WorkflowFactoryImpl)h.getForUpdate(workflowFactoryID);
		try{
			wf.getModel().owners.putAll(owners);
		}
		finally{
			h.persist(wf);
		}
	}

}
