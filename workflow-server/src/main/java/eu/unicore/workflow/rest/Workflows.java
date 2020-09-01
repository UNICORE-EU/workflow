package eu.unicore.workflow.rest;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.chemomentum.dsws.ConversionResult;
import org.chemomentum.dsws.WorkflowFactoryHomeImpl;
import org.chemomentum.dsws.WorkflowFactoryImpl;
import org.chemomentum.dsws.WorkflowHome;
import org.chemomentum.dsws.WorkflowInstance;
import org.chemomentum.dsws.WorkflowModel;
import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.wsrflite.ExternalSystemConnector;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.util.Log;
import eu.unicore.workflow.json.Delegate;
import eu.unicore.workflow.pe.PEConfig;
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
			String id = null;

			JSONObject j = new JSONObject(json);
			String name = j.optString("name",null);
			String ttDef = j.optString("terminationTime",null);
			String dialect = Delegate.DIALECT;

			Calendar tt = null;
			if(ttDef != null){
				Date d = UnitParser.extractDateTime(ttDef);
				tt = Calendar.getInstance();
				tt.setTime(d);
			}
			String storageURL = j.optString("storageURL",null);
			factory = getFactory();
			
			String[] tags = getTags(j);
			
			id = factory.createNewWorkflow(name, tt, storageURL, tags);

			boolean haveWFSubmission = !Boolean.parseBoolean(j.optString("createOnly",null));
			if(haveWFSubmission) {
				newWF = (WorkflowInstance)kernel.getHome("WorkflowManagement").getForUpdate(id);
				ConversionResult cr = newWF.submit(dialect, j, storageURL);
				if(cr.hasConversionErrors()) {
					StringBuilder msg = new StringBuilder();
					msg.append("Could not submit workflow. Workflow contains errors.\n");
					int i = 1;
					for (String s : cr.getConversionErrors()) {
						msg.append(i + ": " + s + "\n");
						i++;
					}
					return createErrorResponse(HttpStatus.SC_BAD_REQUEST, msg.toString());
				}
			}
			String location = getBaseURL()+"/"+id;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create workflow", ex, logger);
		}
		finally{
			if(factory !=null){
				kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME).persist(factory);
			}
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
	 * job list as separate sub-resource
	 */
	@GET
	@Path("/{uniqueID}/jobs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJobs(@QueryParam("offset") @DefaultValue(value="0") int offset, 
			@QueryParam("num") @DefaultValue(value="200") int num) throws Exception {
		try{
			WorkflowContainer wfc = PEConfig.getInstance().getPersistence().read(resourceID);
			Collection<String>jobs = wfc.collectJobs();
			List<String> toRender = null;
			if(jobs instanceof List)toRender = (List<String>)jobs;
			else {
				toRender = new ArrayList<String>();
				toRender.addAll(jobs);
			}
			PagingHelper ph = new PagingHelper(getBaseURL()+"/"+resourceID+"/jobs", "", "jobs");
			JSONObject o = ph.renderJson(offset, num, toRender);
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
				logger.info("Workflow <"+job.getUniqueID()+"> resumed with parameters "+params);
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
				if(logger.isDebugEnabled()) {
					logger.debug("["+resourceID+"] job <"+jobURL+"> is <"+status+">");
				}
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
		Map<String,Object>props = new HashMap<String, Object>();
		Client c = AuthZAttributeStore.getClient();
		props.put("dn", c.getDistinguishedName());
		Role r = c.getRole();
		if(r!=null){
			Map<String,Object>rProps = new HashMap<String, Object>();
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
		
		Map<String,Object>connectors = new HashMap<String, Object>();  
		for(ExternalSystemConnector ec: kernel.getExternalSystemConnectors()){
			connectors.put(ec.getExternalSystemName(), ec.getConnectionStatus());
		}
		props.put("externalConnections", connectors);
		
		try {
			String version = this.getClass().getPackage().getSpecificationVersion();
			props.put("version", version);
		}catch(Exception ex){}
		return props;
	}
	
	@Override
	protected void updateLinks() {
		super.updateLinks();
		links.add(new Link("action:abort",getBaseURL()+"/"+resource.getUniqueID()+"/actions/abort","Abort"));
		links.add(new Link("action:continue",getBaseURL()+"/"+resource.getUniqueID()+"/actions/continue","Continue"));
		links.add(new Link("action:callback",getBaseURL()+"/"+resource.getUniqueID()+"/actions/callback","Job status callback"));
		links.add(new Link("jobs",getBaseURL()+"/"+resource.getUniqueID()+"/jobs","Job list"));
	}

	synchronized WorkflowFactoryImpl getFactory() throws PersistenceException {
		Home home = kernel.getHome(WorkflowFactoryHomeImpl.SERVICE_NAME);
		Client client = AuthZAttributeStore.getClient();
		List<String> factories = home.getAccessibleResources(client);
		if(factories == null || factories.size() == 0){
			throw new AuthorisationException("There are no accessible workflow factories for: " +client+
					" Please check your security setup!");
		}
		return (WorkflowFactoryImpl)home.getForUpdate(factories.get(0));
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
