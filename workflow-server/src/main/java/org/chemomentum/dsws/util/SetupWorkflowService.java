package org.chemomentum.dsws.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;
import org.chemomentum.dsws.ConsolidateWorkflowOwners;
import org.chemomentum.dsws.WorkflowFactoryHomeImpl;
import org.chemomentum.dsws.WorkflowFactoryImpl;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.fzj.unicore.persist.PersistenceFactory;
import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.persist.impl.LockSupport;
import de.fzj.unicore.uas.UAS;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.registry.RegistryHandler;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
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
import eu.unicore.workflow.pe.model.PauseActivity;
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
import eu.unicore.workflow.pe.xnjs.PauseActivityProcessor;
import eu.unicore.workflow.pe.xnjs.RepeatGroupProcessor;
import eu.unicore.workflow.pe.xnjs.RoutingActivityProcessor;
import eu.unicore.workflow.pe.xnjs.WhileGroupProcessor;
import eu.unicore.workflow.pe.xnjs.XNJSProcessEngine;
import eu.unicore.workflow.rest.WorkflowServices;

/**
 * the run() method creates a workflow service instance, and
 * set various config options
 * 
 * @author schuller
 */
public class SetupWorkflowService implements Runnable{

	private final static Logger logger=Log.getLogger(Log.SERVICES,SetupWorkflowService.class);

	public static final String VERSION = UAS.getVersion(WorkflowFactoryImpl.class);

	private final Kernel kernel;
	private final MetricRegistry registry;
	
	public SetupWorkflowService(Kernel kernel){
		this(kernel, kernel.getMetricRegistry());
	}

	public SetupWorkflowService(Kernel kernel, MetricRegistry registry){
		this.kernel = kernel;
		this.registry = registry;
	}

	protected void init() throws Exception{
		if(PEConfig.getInstance().getKernel()==null){
			PEConfig.getInstance().setKernel(kernel);
		}

		WorkflowProperties wp=kernel.getAttribute(WorkflowProperties.class);
		if(wp==null){
			wp=new WorkflowProperties(kernel.getContainerProperties().getRawProperties());
			kernel.addConfigurationHandler(WorkflowProperties.class, wp);
		}
		
		String v=VERSION!=null?VERSION:"DEVELOPMENT";
		String header="UNICORE Workflow service, version "+v
				+"\nBased on developments in the EU-funded Chemomentum project (http://www.chemomentum.org)";
		logger.info(header);
		
		if(PEConfig.getInstance().getProcessEngine()!=null){
			logger.info("Process engine already initialised, skipping init.");
			return;
		}
		XNJS xnjs = setupXNJS();
		xnjs.start();
	}

	protected XNJS setupXNJS() throws Exception {

		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().putAll(kernel.getContainerProperties().getRawProperties());
		cs.setMetricRegistry(registry);
		cs.addModule(new WFEngineModule(cs.getProperties(), kernel));
		
		XNJS xnjs=new XNJS(cs);
		configureProcessing(xnjs);
		
		//configure persistence
		PersistenceProperties pp = kernel.getPersistenceProperties();
		PEConfig.getInstance().setPersistence(PersistenceFactory.get(pp).getPersist(WorkflowContainer.class));
		PEConfig.getInstance().setLocationStore(PersistenceFactory.get(pp).getPersist(Locations.class));
		
		PEConfig.getInstance().setProcessEngine(new XNJSProcessEngine(xnjs));
		PEConfig.getInstance().setCallbackProcessor(new CallbackProcessorImpl(xnjs));
		logger.info("Callback URL configured: "+PEConfig.getInstance().getCallbackURL());
		
		if(PEConfig.getInstance().getRegistry()==null){
			String type="external";
			//configure registry
			String url = null;
			RegistryHandler rh=kernel.getAttribute(RegistryHandler.class);
			if(rh.usesExternalRegistry()) {
				url = rh.getExternalRegistryURLs()[0];
			}
			else {
				type="internal";
				url = kernel.getContainerProperties().getContainerURL()+"/rest/registries/default_registry";
			}
			if(url.contains("/services/Registry")){
				url = convertToREST(url);
				logger.info("Using converted Registry URL "+url);
			}

			eu.unicore.client.registry.IRegistryClient registry = 
					new eu.unicore.client.registry.RegistryClient(url, kernel.getClientConfiguration(), null);
			PEConfig.getInstance().setRegistry(registry);
			logger.info("Using "+type+" registry <"+url+">");
		}
		

		return xnjs;
	}
	

	private String convertToREST(String soapURL) {
		String base = soapURL.split("/services/")[0];
		String regID = soapURL.split("res=")[1]; 
		return base+"/rest/registries/"+regID;
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
				PauseActivity.ACTION_TYPE,
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
				PauseActivityProcessor.class.getName(),
		};

		assert types.length==procs.length;

		for(int i=0;i<types.length;i++){
			if(!xnjs.haveProcessingFor(types[i])){
				xnjs.setProcessingChain(types[i],null , new String[]{procs[i]});
				logger.debug("XNJS config file fix: added XNJS processor definition for "+types[i]);
			}	
		}

	}

	public void run() {
		try{
			init();
			Map<String,String>owners=addDefaultWorkflow();
			WorkflowServices.publish(kernel);
			new ConsolidateWorkflowOwners(WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID, owners, kernel).run();
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	public Map<String,String> addDefaultWorkflow()throws Exception{
		Home h=kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		if(h!=null){
			//get "global" lock
			LockSupport ls=kernel.getPersistenceManager().getLockSupport();
			Lock lock=ls.getOrCreateLock(SetupWorkflowService.class.getName());
			if(lock.tryLock()){
				try{
					//check if instance already exists
					try{
						WorkflowFactoryImpl wf=(WorkflowFactoryImpl)h.get(WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID);
						return wf.getModel().getOwners();
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
			logger.error("The <"+WorkflowFactoryHomeImpl.SERVICE_NAME+"> service is not deployed. Could not add a default workflow service instance.");
		}
		return new HashMap<String, String>();
	}

	private void doCreateWorkflowFactory()throws Exception{
		Home h=kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		InitParameters init = new InitParameters(WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID, TerminationMode.NEVER);
		h.createResource(init);
		logger.info("Added "+WorkflowFactoryHomeImpl.DEFAULT_RESOURCEID+" resource to workflow submission service.");
	}
	
}
