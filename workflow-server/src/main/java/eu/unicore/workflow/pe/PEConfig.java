package eu.unicore.workflow.pe;

import de.fzj.unicore.persist.Persist;
import eu.unicore.services.Kernel;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.ClientConfigProvider;

/**
 * local configuration of the process engine
 *  
 * TODO many things need to go to a properties file 
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
	
	private String callbackURL;

	private Kernel kernel;
	
	private IRegistryClient registry;
	
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
	
	public String getCallbackURL(){
		return callbackURL;
	}
	
	public void setCallbackURL(String url){
		callbackURL=url;
	}

	public void setKernel(Kernel kernel){
		this.kernel=kernel;
	}
	
	public Kernel getKernel(){
		return kernel;
	}
	
	public IRegistryClient getRegistry() {
		return registry;
	}

	public void setRegistry(IRegistryClient registry) {
		this.registry = registry;
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
