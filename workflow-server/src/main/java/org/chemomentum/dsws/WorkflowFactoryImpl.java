package org.chemomentum.dsws;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import de.fzj.unicore.wsrflite.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.workflow.json.Delegate;

/**
 * Implementation of the workflow submission service
 *
 * @author schuller
 */
public class WorkflowFactoryImpl extends BaseResourceImpl {

	private static final Logger logger=Log.getLogger(Log.SERVICES,WorkflowFactoryImpl.class);

	/**
	 * maps dialect URIs to DSLDelegates that do the actual DSL processing
	 */
	private static final Map<String, DSLDelegate>dslDelegates=new HashMap<String, DSLDelegate>();

	static {
		dslDelegates.put(Delegate.DIALECT, new Delegate());
	}
	
	public WorkflowFactoryImpl(){
		super();
	}
	
	public String createNewWorkflow(String name, Calendar tt, String storageURL, String...tags) throws Exception {
		String uid = Utilities.newUniqueID();
		String clientName = getClient().getDistinguishedName();
		logger.info("Creating new workflow instance <"+uid+"> for client "+clientName);
		Home workflowHome = kernel.getHome(WorkflowHome.SERVICE_NAME);
		WorkflowInitParameters init = new WorkflowInitParameters(uid, tt);
		init.parentUUID = getUniqueID();
		init.workflowName = name;
		init.storageURL = storageURL;
		init.initialTags = tags;
		String id=workflowHome.createResource(init);
		getModel().owners.put(id, clientName);
		return id;
	}

	@Override
	public WorkflowFactoryModel getModel(){
		return (WorkflowFactoryModel)model;
	}

	@Override
	public void initialise(InitParameters initObjs) throws Exception {
		if(model==null){
			model=new WorkflowFactoryModel();
		}
		super.initialise(initObjs);
	}

	@Override
	public void processMessages(PullPoint p){
		//check for deleted workflow instances and remove them...
		try{
			while(p.hasNext()){
				String m=(String)p.next().getBody();
				logger.trace("Read: "+m);
				if(m.startsWith("deleted:")){
					String id=m.substring(m.indexOf(":")+1);
					logger.debug("Removing workflow instance with ID "+id+"...");
					getModel().removeChild(id);
					getModel().owners.remove(id);
				}
			}
		}catch(Exception e){
			Log.logException("Error in customPostActivate()",e,logger);
		}
	}

	public String getOwnerForWorkflow(String workflowID){
		return getModel().owners.get(workflowID);
	}

	public static void addDelegate(String dialect, DSLDelegate delegate){
		dslDelegates.put(dialect, delegate);
	}

	public static DSLDelegate getDelegate(String dialect){
		return dslDelegates.get(dialect);
	}

	public static String[] getDialects(){
		return (String[])dslDelegates.keySet().toArray(new String[0]);
	}

}
