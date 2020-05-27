package eu.unicore.workflow.pe.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.persist.Persist;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.workflow.Constants;
import eu.unicore.workflow.pe.PEConfig;

/**
 * Stage-out processing for workflow files. <br/>
 * 
 * This has two parts: <br/>
 * 1) Before launching the job, stage-out to workflow files must be filtered out <br/>
 * 
 * 2) After the job has finished, all workflow files need to be registered with their
 * physical location (the job working directory) <br/>
 * 
 * @author schuller
 */
public class StageOutProcessor {

	private final String workingDirectoryURL;

	private final String workflowID;

	private final String user;

	public StageOutProcessor(String wfID, String workingDirectoryURL, String user) {
		this.workflowID = wfID;
		this.workingDirectoryURL = workingDirectoryURL;
		this.user = user;
	}

	/**
	 * filters out stage-out directives to wf:// URLs that are 
	 * really only file location registrations
	 * 
	 * @param exports
	 * @return stage-outs to physical storage
	 */
	public JSONArray preprocess(JSONArray exports) throws Exception {
		JSONArray results = new JSONArray();
		if(exports==null)return results;
		for(int i=0; i<exports.length(); i++) {
			JSONObject in = exports.getJSONObject(i);
			String target = in.getString("To");
			if(!isLogicalFileName(target)) {
				results.put(in);
			}
		}
		return results;
	}


	public void registerOutputs(JSONArray exports) throws Exception {
		JSONArray toRegister = new JSONArray();
		if(exports==null)return;
		for(int i=0; i<exports.length(); i++) {
			JSONObject in = exports.getJSONObject(i);
			String target = in.getString("To");
			if(isLogicalFileName(target)) {
				toRegister.put(in);
			}
		}
		register(toRegister);
	}

	protected void register(JSONArray outputs) throws Exception {
		if(outputs.length()==0)return;
		Persist<Locations>p = PEConfig.getInstance().getLocationStore();
		Locations l = null;
		try{
			l = p.getForUpdate(workflowID, 30, TimeUnit.SECONDS);
			Map<String,String> locationMap  =  l.getLocations();

			for(int i=0; i<outputs.length(); i++) {
				JSONObject in = outputs.getJSONObject(i);
				String target = in.getString("To");
				String source = in.getString("From");
				List<String>resolved = resolve(source);
				boolean hasWildcards = hasWildcards(source);
				for(String r: resolved) {
					if(hasWildcards) {
						locationMap.put(target+"/"+(new File(r).getName()), r);
					}
					else {
						locationMap.put(target, r);
					}
				}
			}
		}		
		finally {
			if(l!=null) {
				p.write(l);
			}
		}
	}

	private StorageClient storageClient;
	
	protected List<String> resolve(String source) throws Exception {
		if(storageClient==null) {
			storageClient = new StorageClient(new Endpoint(workingDirectoryURL), 
					PEConfig.getInstance().getKernel().getClientConfiguration(), 
					PEConfig.getInstance().getAuthCallback(user));
		}
		
		List<String> resolved = new ArrayList<>();
		if(!source.startsWith("/"))source="/"+source;
		
		if(hasWildcards(source)) {
			// TBD
		}
		else {
			resolved.add(workingDirectoryURL+"/files"+source);
		}
		if(resolved.size()==0)throw new FileNotFoundException("No files matching <"+source+">!");
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
