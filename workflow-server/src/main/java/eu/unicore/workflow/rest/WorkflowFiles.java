package eu.unicore.workflow.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.chemomentum.dsws.WorkflowInstance;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;

/**
 * A very simplistic file catalog that stores names and physical locations for files used in a 
 * workflow
 * 
 * @author schuller
 */
public class WorkflowFiles extends RESTRendererBase {

	private static final Logger logger = Log.getLogger("unicore.rest", WorkflowFiles.class);

	final Kernel kernel;
	final WorkflowInstance wf;

	public WorkflowFiles(Kernel kernel, WorkflowInstance wf, String baseURL){
		this.wf = wf;
		this.kernel = kernel;
		this.baseURL = baseURL;
	}



	/**
	 * query file location(s)
	 */
	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response query(@PathParam("path")String path) 
			throws Exception {
		try{
			if(path==null || path.isEmpty())path="/";
			Locations locations = PEConfig.getInstance().getLocationStore().read(wf.getUniqueID());
			// TODO filter according to path parameter
			JSONObject o = JSONUtil.asJSON(locations.getLocations());
			return Response.ok(o.toString(), MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Error listing workflow files matching'"+path+"'", ex, logger);
		}
	}

	/**
	 * register location
	 */
	@PUT
	@Path("/register")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register(String jsonString) 
			throws Exception {
		Locations locations = null;
		try {
			locations = PEConfig.getInstance().getLocationStore().read(wf.getUniqueID());
			JSONObject o = new JSONObject(jsonString);
			@SuppressWarnings("unchecked")
			Iterator<String> keys = (Iterator<String>)o.keys();
			while(keys.hasNext()) {
				String key = keys.next();
				String loc = o.getString(key);
				if(!key.startsWith("wf:"))key="wf:"+key;
				locations.getLocations().put(key, loc);
			}
			return Response.ok().build();
		}
		catch(Exception e){
			return handleError("Cannot register file(s)", e, logger);
		}finally {
			if(locations!=null) {
				PEConfig.getInstance().getLocationStore().write(locations);
			}
		}
	}

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		Map<String,Object> props = new HashMap<>();
		return props;
	}



	@Override
	protected void updateLinks() {
		links.add(new Link("parent",RESTUtils.makeHref(kernel, "workflows/", wf.getUniqueID()),
				"Parent Workflow"));
	}

}
