package eu.unicore.workflow.pe.util;

import eu.unicore.security.wsutil.client.authn.ClientConfigurationProviderImpl;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 *
 * @author schuller
 */
public class ClientConfigProvider extends ClientConfigurationProviderImpl {

	public ClientConfigProvider(IClientConfiguration baseConfig){
		super();
		setBasicConfiguration(baseConfig);
	}

	public IClientConfiguration getClientConfiguration(String url) {
		if (url == null)throw new IllegalArgumentException("Service URL must be always given");
		return getBasicClientConfiguration().clone();
	}
}