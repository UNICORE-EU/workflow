package org.chemomentum.dsws;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.util.Log;

public class WorkflowHome extends DefaultHome {

	public static final String SERVICE_NAME = "WorkflowManagement";

	@Override
	protected Resource doCreateInstance() {
		return new WorkflowInstance();
	}

	/**
	 * post-restart stuff
	 */
	public void run(){
		try{
			//for each deployed instance, run the postRestart() method
			for(String id: getStore().getUniqueIDs()){
				try(WorkflowInstance i=(WorkflowInstance)getForUpdate(id)){
					i.postRestart();
				}
			}
		}catch(Exception ex){
			Log.logException("Error running port-server start code", ex);
		}
	}
}
