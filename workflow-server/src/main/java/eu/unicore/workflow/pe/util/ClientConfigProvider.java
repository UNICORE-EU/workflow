package eu.unicore.workflow.pe.util;

import de.fzj.unicore.uas.security.WSRFClientConfigurationProviderImpl;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 *
 * @author schuller
 */
public class ClientConfigProvider extends WSRFClientConfigurationProviderImpl {

	public ClientConfigProvider(IClientConfiguration baseConfig){
		super();
		setBasicConfiguration(baseConfig);
	}
	
	@Override
	public IClientConfiguration getClientConfiguration(String serviceUrl, String serviceIdentity, 
			DelegationSpecification delegate) throws Exception
	{
		if (serviceUrl == null)
			throw new IllegalArgumentException("Service URL must be always given");
	
		return getBasicClientConfiguration().clone();
	}
	
	public IClientConfiguration getClientConfiguration(String url) {
		if (url == null)
			throw new IllegalArgumentException("Service URL must be always given");
	
		return getBasicClientConfiguration().clone(); 
	}
}