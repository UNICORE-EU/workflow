package eu.unicore.workflow.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;
import org.chemomentum.dsws.WorkflowFactoryHomeImpl;
import org.chemomentum.dsws.WorkflowFactoryImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.rest.registry.RegistryHandler;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ForGroup;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.model.RepeatGroup;
import eu.unicore.workflow.pe.model.RoutingActivity;
import eu.unicore.workflow.pe.model.WhileGroup;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.ActivityGroupProcessor;
import eu.unicore.workflow.pe.xnjs.CallbackProcessorImpl;
import eu.unicore.workflow.pe.xnjs.DeclarationActivityProcessor;
import eu.unicore.workflow.pe.xnjs.ForGroupProcessor;
import eu.unicore.workflow.pe.xnjs.HoldActivityProcessor;
import eu.unicore.workflow.pe.xnjs.JSONExecutionActivityProcessor;
import eu.unicore.workflow.pe.xnjs.ModificationActivityProcessor;
import eu.unicore.workflow.pe.xnjs.NullECM;
import eu.unicore.workflow.pe.xnjs.RepeatGroupProcessor;
import eu.unicore.workflow.pe.xnjs.RoutingActivityProcessor;
import eu.unicore.workflow.pe.xnjs.WhileGroupProcessor;
import eu.unicore.workflow.pe.xnjs.XNJSProcessEngine;
import eu.unicore.workflow.rest.WorkflowServices;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.persistence.JDBCActionStoreFactory;

/**
 * the run() method creates a workflow service instance, and
 * set various config options
 * 
 * @author schuller
 */
public class WorkflowStartupTask implements Runnable{

	private final static Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, WorkflowStartupTask.class);

	public static final String VERSION = Kernel.getVersion(WorkflowFactoryImpl.class);

	private final Kernel kernel;

	public WorkflowStartupTask(Kernel kernel){
		this.kernel = kernel;
	}

	protected void init() throws Exception{
		if(PEConfig.getInstance().getKernel()==null){
			PEConfig.getInstance().setKernel(kernel);
		}
		WorkflowProperties wp=kernel.getAttribute(WorkflowProperties.class);
		if(wp==null){
			Properties rawProperties = kernel.getContainerProperties().getRawProperties();
			// handle special case of new libs with older properties file
			if(!hasWorkflowSettings(rawProperties)) {
				// assume we are "internal" if this looks like a UNICORE/X
				boolean isInternal = kernel.getDeploymentManager().isFeatureEnabled("Base");
				rawProperties.put("workflow.internalMode", String.valueOf(isInternal));
				logger.info("No configuration for Workflow feature found. Setting internal mode = {}", isInternal);
			}
			wp = new WorkflowProperties(rawProperties);
			kernel.addConfigurationHandler(wp);
			kernel.setAttribute(WorkflowProperties.class, wp);
		}
		logger.info("UNICORE Workflow service, version {}", VERSION);
		if(PEConfig.getInstance().getProcessEngine()!=null){
			return;
		}
		setupXNJS().start();
	}

	private boolean hasWorkflowSettings(Properties properties) {
		 if(properties.get("workflow.internalMode")!=null)return true;
		 for(Object key: properties.keySet()) {
			 if(String.valueOf(key).startsWith("workflow."))return true;
		 }
		 return false;
	}

	protected XNJS setupXNJS() throws Exception {
		WorkflowProperties wp = kernel.getAttribute(WorkflowProperties.class);
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().putAll(wp.getRawProperties());
		cs.addModule(new WFEngineModule(cs.getProperties(), kernel));
		String xnjsID = wp.isInternal()? "wf" : null;
		XNJS xnjs = new XNJS(cs, xnjsID);
		configureProcessing(xnjs);

		PersistenceProperties pp = kernel.getPersistenceProperties();
		PEConfig.getInstance().setPersistence(PersistenceFactory.get(pp).getPersist(WorkflowContainer.class));
		PEConfig.getInstance().setLocationStore(PersistenceFactory.get(pp).getPersist(Locations.class));

		PEConfig.getInstance().setProcessEngine(new XNJSProcessEngine(xnjs));
		PEConfig.getInstance().setCallbackProcessor(new CallbackProcessorImpl(xnjs));

		String type="standard";
		RegistryHandler rh = RegistryHandler.get(kernel);
		rh.getExternalRegistryClient();
		if(wp.isInternal() || !rh.usesExternalRegistry()) {
			type="internal";
			PEConfig.getInstance().setInternalMode(true);
		}
		logger.info("Workflow engine running in <{}> mode.", type);
		return xnjs;
	}

	public static class WFEngineModule extends AbstractModule {

		protected final Kernel kernel;

		protected final Properties properties;

		protected final WorkflowProperties workflowProperties;

		public WFEngineModule(Properties properties, Kernel kernel){
			this.kernel = kernel;
			this.properties = properties;
			this.workflowProperties = new WorkflowProperties(properties);
		}

		@Provides
		public WorkflowProperties getWorkflowProperties(){
			return workflowProperties;
		}

		@Provides
		public Kernel getKernel(){
			return kernel;
		}

		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
			bind(IExecutionContextManager.class).to(NullECM.class);
			bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
		}

	}

	public static void configureProcessing(XNJS xnjs){

		String[]types={
				ActivityGroup.ACTION_TYPE,
				ForGroup.ACTION_TYPE, 
				WhileGroup.ACTION_TYPE, 
				RepeatGroup.ACTION_TYPE,
				JSONExecutionActivity.ACTION_TYPE,
				ModifyVariableActivity.ACTION_TYPE, 
				DeclareVariableActivity.ACTION_TYPE,
				RoutingActivity.ACTION_TYPE,
				HoldActivity.ACTION_TYPE,
		};

		String[]procs={
				ActivityGroupProcessor.class.getName(),
				ForGroupProcessor.class.getName(),
				WhileGroupProcessor.class.getName(),
				RepeatGroupProcessor.class.getName(),
				JSONExecutionActivityProcessor.class.getName(),
				ModificationActivityProcessor.class.getName(),
				DeclarationActivityProcessor.class.getName(),
				RoutingActivityProcessor.class.getName(),
				HoldActivityProcessor.class.getName(),
		};

		assert types.length==procs.length;

		for(int i=0;i<types.length;i++){
			if(!xnjs.haveProcessingFor(types[i])){
				xnjs.setProcessingChain(types[i],null , new String[]{procs[i]});
				logger.debug("XNJS config file fix: added XNJS processor definition for {}", types[i]);
			}	
		}

	}

	@Override
	public void run() {
		try{
			init();
			addDefaultWorkflow();
			WorkflowServices.publish(kernel);
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	public Map<String,String> addDefaultWorkflow()throws Exception{
		Home h=kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		if(h!=null){
			//get "global" lock
			LockSupport ls=kernel.getPersistenceManager().getLockSupport();
			Lock lock = ls.getOrCreateLock(WorkflowStartupTask.class.getName());
			if(lock.tryLock()){
				try{
					//check if instance already exists
					try{
						h.get(WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID);
					}catch(ResourceUnknownException e){
						//does not exist, so create it
						doCreateWorkflowFactory();
					}
				}finally{
					lock.unlock();
				}
			}
		}
		else{
			logger.error("The <{}> service is not deployed. Could not add a default workflow service instance.", WorkflowFactoryHomeImpl.SERVICE_NAME);
		}
		return new HashMap<>();
	}

	private void doCreateWorkflowFactory()throws Exception{
		Home h=kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		InitParameters init = new InitParameters(WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID, TerminationMode.NEVER);
		h.createResource(init);
		logger.debug("Added {} resource to workflow submission service.", WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID);
	}

}
