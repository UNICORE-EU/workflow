package org.chemomentum.dsws;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.impl.DefaultHome;
import eu.unicore.util.Log;

/**
 * the "home" class for the workflow submission
 * 
 * @author schuller
 */
public class WorkflowFactoryHomeImpl extends DefaultHome {
	
	public static final String DEFAULT_RESOURCEID="default_workflow_submission";

	public static final String SERVICE_NAME = "WorkflowFactory";
	
	@Override
	protected Resource doCreateInstance() {
		return new WorkflowFactoryImpl();
	}

	/**
	 * post-restart stuff
	 */
	public void run(){
		try{
			//for each deployed instance, run the postRestart() method
			for(String id: getStore().getUniqueIDs()){
				WorkflowFactoryImpl i=(WorkflowFactoryImpl)getForUpdate(id);
				try{
					i.postRestart();
				}finally{
					if(i!=null){
						getStore().persist(i);
					}
				}
			}
		}catch(Exception ex){
			Log.logException("Error running port-server start code", ex);
		}
	}
	
}
