package eu.unicore.workflow.rest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.chemomentum.dsws.WorkflowInstance;
import org.json.JSONObject;

import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

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
		InputStream is = null;
		try{
			if(path==null || path.isEmpty())path="/";
			
			
			throw new Exception("not yet");
		}catch(Exception ex){
			de.fzj.unicore.xnjs.util.IOUtils.closeQuietly(is);
			return handleError("Error downloading from '"+path+"'", ex, logger);
		}
	}

	/**
	 * create new record(s)
	 */
	@POST
	@Path("/register")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register(@PathParam("path")String path, String jsonString) 
			throws Exception {

		if(path == null || path.isEmpty())path="/";
		try{
			JSONObject j = new JSONObject(jsonString);
			
			return Response.ok().build();
		}
		catch(Exception e){
			return handleError("Cannot create record <"+path+">", e, logger);
		}
	}

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		Map<String,Object> props = new HashMap<>();
		return props;
	}



	@Override
	protected void updateLinks() {
		links.add(new Link("parentStorage",RESTUtils.makeHref(kernel, "workflows/", wf.getUniqueID()),
				"Parent Workflow"));
	}

}
