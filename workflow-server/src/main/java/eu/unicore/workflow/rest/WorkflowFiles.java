package eu.unicore.workflow.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.chemomentum.dsws.WorkflowInstance;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A very simplistic file catalog that stores names and physical locations for files used in a 
 * workflow
 * 
 * @author schuller
 */
public class WorkflowFiles {

	private static final Logger logger = Log.getLogger("unicore.rest", WorkflowFiles.class);

	final Kernel kernel;
	final WorkflowInstance wf;
	final String baseURL;

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
			JSONObject o ;
			Map<String, String>loc = locations.getLocations();
			if(path!="/") {
				if(!path.startsWith("wf:")) {
					path="wf:"+path;
				}
				Pattern p = compilePattern(path);
				o = new JSONObject();
				for(String name: loc.keySet()) {
					if(p.matcher(name).find()) {
						o.put(name, loc.get(name));
					}
				}
			}else {
				o = JSONUtil.asJSON(loc);
			}
			return Response.ok(o.toString(), MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return RESTRendererBase.handleError("Error listing workflow files matching'"+path+"'", ex, logger);
		}
	}
	
	private Pattern compilePattern(String expr){
		StringBuilder pattern=new StringBuilder();
		pattern.append(expr.replace(".","\\.").replace("*", "[^/]*").replace("?", "."));
		pattern.append("\\Z");
		return Pattern.compile(pattern.toString());
	}
	
	/**
	 * register location
	 */
	@PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response register(String jsonString) 
			throws Exception {
		Locations locations = null;
		try {
			locations = PEConfig.getInstance().getLocationStore().read(wf.getUniqueID());
			JSONObject o = new JSONObject(jsonString);
			Iterator<String> keys = o.keys();
			JSONObject reply = new JSONObject();
			while(keys.hasNext()) {
				String key = keys.next();
				String loc = o.getString(key);
				if(!key.startsWith("wf:"))key="wf:"+key;
				locations.getLocations().put(key, loc);
				reply.put(key, loc);
			}
			return Response.ok().entity(reply.toString()).build();
		}
		catch(Exception e){
			return RESTRendererBase.handleError("Cannot register file(s)", e, logger);
		}finally {
			if(locations!=null) {
				PEConfig.getInstance().getLocationStore().write(locations);
			}
		}
	}

}
