package eu.unicore.workflow.features;

import java.util.ArrayList;
import java.util.List;

import org.chemomentum.dsws.WorkflowFactoryHomeImpl;
import org.chemomentum.dsws.WorkflowHome;

import de.fzj.unicore.wsrflite.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.utils.deployment.FeatureImpl;
import eu.unicore.services.rest.RestService;
import eu.unicore.workflow.rest.WorkflowServices;

/**
 * Workflow engine feature. Deploys the REST frontend.
 * 
 * @author schuller
 */
public class WorkflowEngineFeature extends FeatureImpl {

	public WorkflowEngineFeature(Kernel kernel) {
		this();
		setKernel(kernel);
	}
	
	public WorkflowEngineFeature() {
		this.name = "WorkflowEngine";
		getHomeClasses().put(WorkflowFactoryHomeImpl.SERVICE_NAME, WorkflowFactoryHomeImpl.class);
		getHomeClasses().put(WorkflowHome.SERVICE_NAME, WorkflowHome.class);
	}
	
	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		services.add(new RESTSD(kernel));		
		return services;
	}
		
	public static class RESTSD extends DeploymentDescriptorImpl {

		public RESTSD(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public RESTSD() {
			super();
			this.name = "workflows";
			this.type = RestService.TYPE;
			this.implementationClass = WorkflowServices.class;
		}

	}

}