package eu.unicore.workflow.pe;

import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.persist.Persist;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.ClientConfigProvider;

/**
 * utility for accessing common components
 *
 * @author schuller
 */
public class PEConfig {

	
	private PEConfig(){}
	
	private static PEConfig instance;
	
	private Persist<WorkflowContainer> store;
	
	private Persist<Locations> locations;
	
	private ProcessEngine pe;
	
	public static synchronized PEConfig getInstance(){
		if(instance==null){
			instance=new PEConfig();
		}
		return instance;
	}
	
	private CallbackProcessor cp;

	private Kernel kernel;
	
	private String registryURL;
	
	private boolean keepAllActions=false;
	
	public ProcessEngine getProcessEngine(){
		return pe;
	}
	
	public Persist<WorkflowContainer> getPersistence(){
		return store;
	}
	
	public void setLocationStore(Persist<Locations> locations){
		this.locations = locations;
	}
	
	public Persist<Locations> getLocationStore(){
		return locations;
	}
	
	public void setPersistence(Persist<WorkflowContainer>store){
		this.store=store;
	}
	
	public void setProcessEngine(ProcessEngine pe){
		this.pe=pe;
	}
	
	public synchronized CallbackProcessor getCallbackProcessor(){
		return cp;
	}
	
	public synchronized void setCallbackProcessor(CallbackProcessor cpImpl){
		cp=cpImpl;
	}

	public void setKernel(Kernel kernel){
		this.kernel=kernel;
	}
	
	public Kernel getKernel(){
		return kernel;
	}

	public IRegistryClient getRegistry() {
		return new RegistryClient(registryURL, kernel.getClientConfiguration(), null);
	}

	public void setRegistryURL(String registryURL) {
		this.registryURL = registryURL;
	}
	
	public boolean isKeepAllActions() {
		return keepAllActions;
	}

	/**
	 * for testing only: should the process engine keep ALL actions instead of cleaning up?
	 * @param keepAllActions
	 */
	public void setKeepAllActions(boolean keepAllActions) {
		this.keepAllActions = keepAllActions;
	}
	

	public ClientConfigProvider getConfigProvider() {
		return new ClientConfigProvider(kernel.getClientConfiguration());
	}
	
	
	public IAuthCallback getAuthCallback(String user) throws Exception {
		try{
			return new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
					new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		}catch(Exception ex) {return null;}
	}
}
