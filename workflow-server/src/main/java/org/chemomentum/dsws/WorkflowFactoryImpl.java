package org.chemomentum.dsws;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.messaging.impl.ResourceDeletedMessage;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.json.Delegate;
import eu.unicore.workflow.pe.files.Locations;

/**
 * Implementation of the workflow submission service
 *
 * @author schuller
 */
public class WorkflowFactoryImpl extends BaseResourceImpl {

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, WorkflowFactoryImpl.class);

	/**
	 * maps dialect URIs to DSLDelegates that do the actual DSL processing
	 */
	private static final Map<String, DSLDelegate>dslDelegates = new HashMap<>();

	static {
		dslDelegates.put(Delegate.DIALECT, new Delegate());
	}
	
	public WorkflowFactoryImpl(){
		super();
	}
	
	public String createNewWorkflow(String uid, ConversionResult cr, Locations locations, String name, Calendar tt, String storageURL, String...tags) throws Exception {
		String clientName = getClient().getDistinguishedName();
		logger.info("Creating new workflow instance <{}> for <{}>", uid, clientName);
		Home workflowHome = kernel.getHome(WorkflowHome.SERVICE_NAME);
		WorkflowInitParameters init = new WorkflowInitParameters(uid, tt);
		init.parentUUID = getUniqueID();
		init.workflowName = name;
		init.storageURL = storageURL;
		init.initialTags = tags;
		init.cr = cr;
		init.locations = locations;
		return workflowHome.createResource(init);
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
		try{
			while(p.hasNext()){
				var m = p.next();
				if(m instanceof ResourceDeletedMessage){
					String id = ((ResourceDeletedMessage) m).getDeletedInstance();
					getModel().removeChild(id);
				}
			}
		}catch(Exception e){
			Log.logException("Error processing messages", e, logger);
		}
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
	
	public static ConversionResult convert(String dialect, Object wf, String wfUID, SecurityTokens tokens) {
		DSLDelegate del = WorkflowFactoryImpl.getDelegate(dialect);
		if (del == null) {
			throw new IllegalArgumentException("Dialect <" + dialect
					+ "> not understood");
		}
		return del.convertWorkflow(wfUID, wf, tokens);
	}
}
