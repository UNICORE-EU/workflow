package eu.unicore.workflow.rest;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.chemomentum.dsws.ConversionResult;
import org.chemomentum.dsws.WorkflowFactoryHomeImpl;
import org.chemomentum.dsws.WorkflowFactoryImpl;
import org.chemomentum.dsws.WorkflowHome;
import org.chemomentum.dsws.WorkflowInstance;
import org.chemomentum.dsws.WorkflowModel;
import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Home;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.json.Delegate;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;

/**
 * REST interface to workflows
 *
 * @author schuller
 */
@Path("/")
@USEResource(home="WorkflowManagement")
public class Workflows extends ServicesBase {

	private static final Logger logger = Log.getLogger("unicore.rest", Workflows.class);

	@Override
	protected String getResourcesName(){
		return "workflows";
	}
	
	@Override
	protected String getPathComponent(){
		return "";
	}
	
	/**
	 * create a new workflow
	 * 
	 * @param json - workflow JSON, also containing extra info like workflow name, termination time, storage address
	 * @return address of new resource. If workflow has errors, response will have error info and status '400'
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createWorkflow(String json) throws Exception {
		WorkflowFactoryImpl factory = null;
		WorkflowInstance newWF = null;

		try{
			String newUID = Utilities.newUniqueID();

			JSONObject j = new JSONObject(json);
			String name = j.optString("name", null);
			String ttDef = j.optString("terminationTime", null);
			String dialect = Delegate.DIALECT;

			Calendar tt = null;
			if(ttDef != null){
				Date d = UnitParser.extractDateTime(ttDef);
				tt = Calendar.getInstance();
				tt.setTime(d);
			}
			String storageURL = j.optString("storageURL", null);
			factory = getFactory();

			ConversionResult cr = WorkflowFactoryImpl.convert(dialect, j, newUID, AuthZAttributeStore.getTokens());

			// register inputs
			JSONObject inputs = j.optJSONObject("inputs");
			String inputError = null;
			
			Locations locations = new Locations();
			locations.setWorkflowID(newUID);
			if(!cr.hasConversionErrors() && inputs!=null) {
				try {
					Iterator<String> names = inputs.keys();
					while(names.hasNext()) {
						String logicalName = names.next();
						String location = inputs.getString(logicalName);
						locations.getLocations().put(logicalName, location);
					}
					PEConfig.getInstance().getLocationStore().write(locations);
				}catch(Exception ex) {
					inputError = Log.createFaultMessage("Could not register inputs.", ex);
				}
			}
			
			if(cr.hasConversionErrors() || inputError!=null) {
				StringBuilder msg = new StringBuilder();
				msg.append("Could not submit workflow. Workflow contains errors.\n");
				if(inputError!=null) {
					msg.append(inputError).append("\n");
				}
				int i = 1;
				for (String s : cr.getConversionErrors()) {
					msg.append(i + ": " + s + "\n");
					i++;
				}
				try{
					PEConfig.getInstance().getLocationStore().remove(newUID);
				}catch(Exception e){}
				return createErrorResponse(HttpStatus.SC_BAD_REQUEST, msg.toString());
			}
			String[] tags = getTags(j);
			factory.createNewWorkflow(newUID, cr, locations, name, tt, storageURL, tags);
			String location = getBaseURL()+"/"+newUID;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create workflow", ex, logger);
		}
		finally{
			if(newWF !=null){
				kernel.getHome(WorkflowHome.SERVICE_NAME).persist(newWF);
			}
		}
	}

	/**
	 * handle workflow files as a sub-resource
	 */
	@Path("/{uniqueID}/files")
	public WorkflowFiles getFilesResource() {
		String filesURL = getBaseURL()+"/"+getResourcesName()+"/"+resourceID+"/files";
		return new WorkflowFiles(kernel, getResource(), filesURL);
	}

	/**
	 * job list
	 */
	@GET
	@Path("/{uniqueID}/jobs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJobs(@QueryParam("offset") @DefaultValue(value="0") int offset, 
			@QueryParam("num") @DefaultValue(value="200") int num) throws Exception {
		try{
			WorkflowContainer wfc = PEConfig.getInstance().getPersistence().read(resourceID);
			List<String>jobs = wfc.collectJobs();
			PagingHelper ph = new PagingHelper(getBaseURL()+"/"+resourceID+"/jobs", "", "jobs");
			JSONObject o = ph.renderJson(offset, num, jobs);
			return Response.ok(o.toString(), MediaType.APPLICATION_JSON).build();

		}catch(Exception ex){
			return handleError("Error", ex, logger);
		}
	}
	
	@Override
	protected void doHandleAction(String action, JSONObject json) throws Exception {
		WorkflowInstance job = getResource();
		if("abort".equals(action)){
			job.doAbort();
		}
		else if("continue".equals(action)){
			if(job.canResume()){
				Map<String,String>params = JSONUtil.asMap(json);
				job.doResume(params);
				logger.debug("Workflow <{}> resumed with parameters {}", job.getUniqueID(), params);
			}
			else{
				throw new WebApplicationException("Cannot resume workflow", HttpStatus.SC_CONFLICT);
			}
		}
		else{
			Map<String,String>params = JSONUtil.asMap(json);
			if("callback".equals(action)) {
				String jobURL = params.get("href");
				String status = params.get("status");
				String statusMessage = params.get("statusMessage");
				logger.debug("[{}] job <{}> is <{}>", resourceID, jobURL, status);
				if("RUNNING".equals(status))return;
				else {
					boolean success = "SUCCESSFUL".equals(status);
					PEConfig.getInstance().getCallbackProcessor().handleCallback(resourceID, jobURL, statusMessage, success);
				}
			}
			else throw new IllegalArgumentException("Action '"+action+"' is not known.");
		}
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		renderStatus(props);
		return props;
	}

	protected void renderStatus(Map<String,Object> o) throws Exception{
		WorkflowInstance resource = getResource();
		WorkflowModel model = getModel();
		o.put("name", model.getWorkflowName());
		o.put("status", String.valueOf(resource.getProcessState().getState()));
		o.put("detailedStatus", getDetailedStatus());
		o.put("parameters", resource.getVariableValues());
		o.put("submissionTime", UnitParser.getISO8601().format(model.getSubmissionTime().getTime()));
	}

	protected Map<String,Object>getDetailedStatus() throws Exception {
		return new Delegate().getStatus(resourceID);
	}

	@Override
	public WorkflowModel getModel(){
		return (WorkflowModel)model;
	}

	@Override
	public WorkflowInstance getResource(){
		return (WorkflowInstance)resource;
	}

	@Override
	protected void customizeBaseProperties(JSONObject props) throws Exception {
		props.put("client", renderClientProperties());
		props.put("server", renderServerProperties());
	}

	protected Map<String, Object> renderClientProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		Client c = AuthZAttributeStore.getClient();
		props.put("dn", c.getDistinguishedName());
		Role r = c.getRole();
		if(r!=null){
			Map<String,Object>rProps = new HashMap<>();
			rProps.put("selected",r.getName());
			rProps.put("availableRoles",r.getValidRoles());
			props.put("role",rProps);
		}
		return props;
	}
	
	protected Map<String, Object> renderServerProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		if(kernel.getContainerSecurityConfiguration().getCredential()!=null){
			Map<String,Object>cred = new HashMap<>();
			try{
				X509Certificate cert = kernel.getContainerSecurityConfiguration().getCredential().getCertificate();
				cred.put("dn", cert.getSubjectX500Principal().getName());
				cred.put("issuer", cert.getIssuerX500Principal().getName());
				cred.put("expires", getISODateFormatter().format(cert.getNotAfter()));
			}catch(Exception ex) {}
			props.put("credential", cred);
		}
		List<String>trusted = new ArrayList<>();
		try{
			X509Certificate[] trustedCAs = kernel.getContainerSecurityConfiguration().getValidator().getTrustedIssuers();
			for(X509Certificate c: trustedCAs) {
				trusted.add(c.getSubjectX500Principal().getName());
			}
		}catch(Exception ex) {}
		props.put("trustedCAs",trusted);
		
		List<String>trustedSAML = new ArrayList<>();
		try{
			X509Certificate[] trustedCAs = kernel.getContainerSecurityConfiguration().getTrustedAssertionIssuers().getTrustedIssuers();
			for(X509Certificate c: trustedCAs) {
				trustedSAML.add(c.getSubjectX500Principal().getName());
			}
		}catch(Exception ex) {}
		props.put("trustedSAMLIssuers",trustedSAML);
		
		Map<String,Object>connectors = new HashMap<>();
		for(ExternalSystemConnector ec: kernel.getExternalSystemConnectors()){
			connectors.put(ec.getExternalSystemName(), ec.getConnectionStatus());
		}
		props.put("externalConnections", connectors);

		try {
			String version = this.getClass().getPackage().getSpecificationVersion();
			props.put("version", version);
		}catch(Exception ex){}

		WorkflowProperties wp = kernel.getAttribute(WorkflowProperties.class);
		props.put("engineMode", wp.isInternal()? "internal" : "standard");
		return props;
	}
	
	@Override
	protected void updateLinks() {
		super.updateLinks();
		String base = getBaseURL()+"/"+resource.getUniqueID();
		links.add(new Link("action:abort", base+"/actions/abort", "Abort"));
		links.add(new Link("action:continue", base+"/actions/continue", "Continue"));
		links.add(new Link("action:callback", base+"/actions/callback", "Job status callback"));
		links.add(new Link("jobs",  base+"/jobs", "Job list"));
		links.add(new Link("files", base+"/files", "File list"));
	}

	synchronized WorkflowFactoryImpl getFactory() throws Exception {
		Home home = kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		Client client = AuthZAttributeStore.getClient();
		List<String> factories = home.getAccessibleResources(client);
		if(factories == null || factories.size() == 0){
			throw new AuthorisationException("There are no accessible workflow factories for: " +client+
					" Please check your security setup!");
		}
		return (WorkflowFactoryImpl)home.get(factories.get(0));
	}
	
	public String[] getTags(JSONObject json){
		JSONArray tags = json.optJSONArray("Tags");
		if(tags==null)tags = json.optJSONArray("tags");
		if(tags!=null){
			String[] ret = new String[tags.length()];
			for(int i=0;i<tags.length();i++){
				ret[i]=tags.optString(i);
			}
			return ret;
		}
		return null;
	}

}
