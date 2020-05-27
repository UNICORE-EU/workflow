package eu.unicore.workflow.rest;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import de.fzj.unicore.uas.rest.CoreServices;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.registry.LocalRegistryClient;
import de.fzj.unicore.wsrflite.registry.ServiceRegistryImpl;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryHandler;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.util.Log;

/**
 * REST app dealing with workflows
 * 
 * @author schuller
 */
public class WorkflowServices extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<Class<?>>();
		classes.add(Workflows.class);
		return classes;
	}


	public static boolean isEnabled(Kernel kernel){
		return kernel.getService("workflows")!=null;
	}
	
	public static void publish(Kernel kernel){
		if(!isEnabled(kernel))return;
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			String endpoint = kernel.getContainerProperties().getContainerURL()+"/rest/workflows";
			Map<String,String>content = new HashMap<>();
			content.put(ServiceRegistryImpl.INTERFACE_NAME, "WorkflowServices");
			content.put(ServiceRegistryImpl.INTERFACE_NAMESPACE, "https://www.unicore.eu/rest");
			X509Credential cred = kernel.getContainerSecurityConfiguration().getCredential();
			if(cred!=null){
				StringWriter out = new StringWriter();
				try(JcaPEMWriter writer = new JcaPEMWriter(out)){
					writer.writeObject(cred.getCertificate());
				}catch(Exception ex){
					Log.logException("Cannot write public key", ex, Log.getLogger("unicore.security", CoreServices.class));
				}
				content.put(ServiceRegistryImpl.SERVER_PUBKEY, out.toString());
				content.put(ServiceRegistryImpl.SERVER_IDENTITY, cred.getSubjectName());
				
			}
			lrc.addEntry(endpoint, content, null);
		}catch(Exception ex){
			ex.printStackTrace();
			Log.logException("Could not publish to registry", ex);
		}
	}
	
}
