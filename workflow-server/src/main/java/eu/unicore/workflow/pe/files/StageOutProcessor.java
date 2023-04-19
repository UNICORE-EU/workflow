package eu.unicore.workflow.pe.files;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.persist.Persist;
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

	private FileListEntry remote;

	private FileSet fileSet;

	private StorageClient storageClient;
	
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
				
				getRemoteInfo(source);

				Map<String,String>toRegister = new HashMap<>();
				
				if(remote.isDirectory) {
					doCollectOutputs(toRegister, remote, target, remote.path);
				}
				else {
					if(!source.startsWith("/"))source="/"+source;
					toRegister.put(target, workingDirectoryURL+"/files"+source);
				}
				for(Map.Entry<String, String> e: toRegister.entrySet()) {
					locationMap.put(e.getKey(), e.getValue());
				}
			}
		}		
		finally {
			if(l!=null) {
				p.write(l);
			}
		}
	}
	
	protected void doCollectOutputs(Map<String,String> collection, FileListEntry sourceFolder, 
			String targetFolder, String baseDirectory) throws Exception {
		for (FileListEntry child : storageClient.ls(sourceFolder.path).list(0, SMSBaseImpl.MAX_LS_RESULTS)) {
			String relative = IOUtils.getRelativePath(child.path, sourceFolder.path);
			String target = IOUtils.getNormalizedPath(targetFolder+relative);
			if(child.isDirectory && fileSet.isRecurse())
			{
				doCollectOutputs(collection, child, target, baseDirectory);
			}
			else 
			{
				if(remote.isDirectory && fileSet.matches(child.path)){
					collection.put(target, workingDirectoryURL+"/files"+child.path);
				}
			}
		}
	}

	protected void getRemoteInfo(String source) throws Exception {
		if(storageClient==null) {
			storageClient = new StorageClient(new Endpoint(workingDirectoryURL), 
					PEConfig.getInstance().getKernel().getClientConfiguration(), 
					PEConfig.getInstance().getAuthCallback(user));
		}
		
		if(!FileSet.hasWildcards(source)){
			remote = storageClient.stat(source);
			boolean dir = remote.isDirectory;
			if(dir){
				fileSet = new FileSet(source,true);
			}
			else{
				fileSet = new FileSet(source);
			}
		}
		else{
			// have wildcards
			fileSet = new FileSet(source);
			remote = storageClient.stat(fileSet.getBase());
		}
		if(remote == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}
	
	public static boolean isLogicalFileName(String uri)
	{
		return uri.startsWith(Constants.LOGICAL_FILENAME_PREFIX);
	}

}
