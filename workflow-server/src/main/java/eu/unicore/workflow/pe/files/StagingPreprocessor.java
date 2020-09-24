package eu.unicore.workflow.pe.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
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
				String path = new URI(source).getSchemeSpecificPart();
				String file = new File(path).getName();
				String targetDir = in.getString("To");
				if(hasWildcards(file)){
					Map<String,String>toImport = resolveWildcards(source, targetDir);
					for(Map.Entry<String, String>e: toImport.entrySet()) {
						JSONObject o = new JSONObject(in.toString());
						o.put("From", e.getKey());
						o.put("To", e.getValue());
						results.put(o);
					}
				}
				else {
					in.put("From", resolve(source));
					results.put(in);
				}
			}else {
				// normal stage-in, don't mess with it
				results.put(in);
			}
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
		String resolved = locationMap.get(source);
		if(resolved==null)throw new FileNotFoundException("Workflow file <"+source+"> can not be resolved");
		return resolved;
	}
	
	protected Map<String,String> resolveWildcards(String source, String targetDir) throws Exception {
		if(locationMap==null) {
			locationMap = PEConfig.getInstance().getLocationStore().read(workflowID).getLocations();
		}
		Map<String,String> resolved = new HashMap<>();
		for(String key: locationMap.keySet()) {
			// TODO this is very basic
			if(FilenameUtils.wildcardMatch(key, source)) {
				resolved.put(locationMap.get(key), targetDir+"/"+new File(key).getName());
			}
		}
		if(resolved.size()==0)throw new FileNotFoundException("Workflow file(s) <"+source+"> can not be resolved");
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
