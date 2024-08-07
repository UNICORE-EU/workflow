package eu.unicore.workflow.rest;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

/**
 * callback receiver
 */
@Disabled
public class CallbackReceiverFeature extends FeatureImpl {

	public CallbackReceiverFeature(Kernel kernel) {
		this();
		setKernel(kernel);
	}

	public CallbackReceiverFeature() {
		this.name = "TestingCallbackReceiver";
	}

	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		services.add(new CB(kernel));
	}

	public static class CB extends DeploymentDescriptorImpl {

		public CB(Kernel kernel){
			this();
			setKernel(kernel);
		}

		public CB() {
			super();
			this.name = "callbacks";
			this.type = RestService.TYPE;
			this.implementationClass = CallbackApplication.class;
		}
	}
	
	public static class CallbackApplication extends Application {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<Class<?>>();
			classes.add(Callbacks.class);
			return classes;
		}
	}
	
	@Path("/")
	public static class Callbacks {
		@POST
		@Path("/")
		@Consumes(MediaType.APPLICATION_JSON)
		public void receive(String json) {
			JSONObject notification = new JSONObject(json);
			System.out.println("Callback: "+notification.toString(2));
		}
	}

}