package eu.unicore.workflow.pe.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.workflow.Constants;
import eu.unicore.workflow.pe.PEConfig;

/**
 * workflow file references in stage-ins are resolved 
 * and replaced by their physical counterparts
 * 
 * @author schuller
 */
public class StagingPreprocessor {
	
	private final String workflowID;
	
	public StagingPreprocessor(String wfID) {
		this.workflowID = wfID;
	}

	public JSONArray processImports(JSONArray imports) throws Exception {
		JSONArray results = new JSONArray();
		if(imports==null)return results;
		for(int i=0; i<imports.length(); i++) {
			JSONObject in = imports.getJSONObject(i);
			String source = in.getString("From");
			if(isLogicalFileName(source)) {
				in.put("From", resolve(source));
			}
			results.put(in);
		}
		return results;
	}
	
	
	/**
	 * filter out "exports to workflow file" - which will be handled after the job has finished
	 * @param exports
	 * @return
	 * @throws Exception
	 */
	public JSONArray processExports(JSONArray exports) throws Exception {
		JSONArray results = new JSONArray();
		if(exports==null)return results;
		for(int i=0; i<exports.length(); i++) {
			JSONObject out = exports.getJSONObject(i);
			String target = out.getString("To");
			if(!isLogicalFileName(target)) {
				results.put(out);
			}
		}
		return results;
	}
	
	private Map<String,String> locationMap  = null;
	
	protected String resolve(String source) throws Exception {
		if(locationMap==null) {
			locationMap = PEConfig.getInstance().getLocationStore().read(workflowID).getLocations();
		}
		String resolved = null;
		String path = new URI(source).getSchemeSpecificPart();
		String file = new File(path).getName();
		if(!hasWildcards(file)) {
			resolved = locationMap.get(source);
		}
		else {
			// TBD
		}
		if(resolved==null)throw new FileNotFoundException("Workflow file <"+source+"> can not be resolved");
		return resolved;
	}
	
	public static boolean isLogicalFileName(String uri)
	{
		return uri.startsWith(Constants.LOGICAL_FILENAME_PREFIX);
	}

	public static boolean hasWildcards(String expr) 
	{
		return expr.contains("*")|| expr.contains("?");
	}

}
