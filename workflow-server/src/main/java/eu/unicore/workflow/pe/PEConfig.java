package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.unicore.client.Endpoint;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.client.registry.RegistryClient.ServiceTypeFilter;
import eu.unicore.persist.Persist;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.IRegistry;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.registry.RegistryHandler;
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

	private boolean internalMode = false;

	public static synchronized PEConfig getInstance(){
		if(instance==null){
			instance=new PEConfig();
		}
		return instance;
	}

	private CallbackProcessor cp;

	private Kernel kernel;

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

	public IRegistryClient getRegistry() throws Exception {
		RegistryHandler rh = RegistryHandler.get(kernel);
		if(!internalMode && rh.usesExternalRegistry()) {
			return new RC(rh.getExternalRegistryClient());
		}
		else {
			return new RC(rh.getRegistryClient());
		}
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

	public boolean isInternalMode() {
		return internalMode;
	}

	public void setInternalMode(boolean internalMode) {
		this.internalMode = internalMode;
	}

	public static class RC implements IRegistryClient {

		private final static ServiceListFilter acceptAll = new ServiceListFilter(){
			public boolean accept(Endpoint ep){return true;}
		};

		final IRegistry backend;

		public RC(IRegistry backend) {
			this.backend = backend;
		}

		@Override
		public List<Endpoint> listEntries(ServiceListFilter acceptFilter) throws Exception {
			List<Endpoint>endpoints = new ArrayList<>();
			List<Map<String,String>> e =  backend.listEntries();
			for(int i=0; i<e.size(); i++){
				try{
					Endpoint ep = toEP(e.get(i));
					if(acceptFilter!=null && !acceptFilter.accept(ep))continue;
					endpoints.add(ep);
				}catch(Exception ex){}
			}
			return endpoints;
		}

		@Override
		public List<Endpoint> listEntries() throws Exception {
			return listEntries(acceptAll);
		}

		@Override
		public List<Endpoint> listEntries(String type) throws Exception {
			return listEntries(new ServiceTypeFilter(type));
		}

		@Override
		public String getConnectionStatus() throws Exception {
			return "OK";
		}

		@Override
		public boolean checkConnection() throws Exception {
			return true;
		}

		private Endpoint toEP(Map<String,String>content){
			String url = content.get("href");
			if(url==null)url=content.get("Endpoint");
			Endpoint ep = new Endpoint(url);
			String type = content.get("type");
			if(type==null)type=content.get("InterfaceName");
			ep.setInterfaceName(type);
			ep.setServerPublicKey(content.get(RegistryClient.SERVER_PUBKEY));
			ep.setServerIdentity(content.get(RegistryClient.SERVER_IDENTITY));
			return ep;
		}
	}
}
