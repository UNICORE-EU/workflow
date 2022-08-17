package eu.unicore.workflow.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.junit.Ignore;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;

/**
 * callback receiver
 */
@Ignore
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