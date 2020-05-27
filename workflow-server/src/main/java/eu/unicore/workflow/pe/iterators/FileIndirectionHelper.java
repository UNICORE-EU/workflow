package eu.unicore.workflow.pe.iterators;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;


/**
 * helper to process file indirection:
 * Iteration over multiple files within a for-each loop, which are referenced in
 * another file. Supports
 * 1.) BFT:http.....#/path/to/myfile
 * 2.) c9m:${WORKFLOW_ID}/JobID/myfile
 * Does not support local files (it cannot)
 * 
 * Example:
 * file. myfiles:
 * myfile1_iter1 myfile2_iter1 myfile3_iter1
 * myfile1_iter2 myfile2_iter2 myfile3_iter2
 * ...
 * 
 * 
 * @author strunk
 * @author schuller
 */
public class FileIndirectionHelper {

	private final static Logger logger=Log.getLogger(Log.SERVICES, FileIndirectionHelper.class);

	// the file names to read the real file names from
	private final Collection<Pair<String,Long>> source;

	private final String workflowID;

	private String currentOpenFile = null;

	private Scanner currentFileScanner = null;

	/**
	 * Create a new FileListIterator
	 *  
	 * @param source -  filenames and corresponding sizes
	 * @param workflowID
	 */
	public FileIndirectionHelper(Collection<Pair<String,Long>> source, String workflowID){
		this.source=source;
		this.workflowID=workflowID;
	}


	public Collection<Pair<String,Long>> resolve() {
		Collection<Pair<String,Long>> results = new ArrayList<Pair<String,Long>>();

		// iterate over the given source files which contain the real file names
		Iterator<Pair<String,Long>>sourceIterator=source.iterator();
		while(sourceIterator.hasNext()){
			Pair<String,Long>currentVal=sourceIterator.next();
			String currentFile=currentVal.getM1();
			logger.debug("Opening File: " + currentFile);
			currentOpenFile = getContentsOfUrl(workflowID,currentFile);
			currentFileScanner = new Scanner(currentOpenFile);

			// now iterate over the contents 
			while(currentFileScanner.hasNext()) {

				String iterLine = currentFileScanner.nextLine();
				String[] files = iterLine.split(" ");

				//now we need to resolve these files and that's it.
				logger.debug("Filenames included for staging from FileListIterator: " + iterLine);

				//This filename will mostly look like this:
				//BFT:https://......#/abc/def/ABC
				//or even more likely, like this:
				//c9m:${WORKFLOW_ID}/JobName/InterestingFile
				//
				//This needs to be converted into a FileSet, i.e.:
				//
				//the resolver requires as base:
				// for BFT: everything until the hash#
				// Include is then the full path /abc/def/ABC
				// for c9m: Base: c9m:${WORKFLOW_ID}/JobName/ (WITH SLASH AT END)
				//  include is then just InterestingFile
				// So, first of all: Replace all occurences for WORKFLOW_ID, 
				// then split at last second slash for c9m, at hash for unicore storage


				for (String file : files) {
					FileSet toResolve = reformatStorageURI(file, workflowID);
					//${WORKFLOW_ID} has been substituted and the URI has been cut into base and include, now actual resolving to BFT: starts
					try{
						ResolverFactory.Resolver r=ResolverFactory.getResolver(toResolve.base);
						results.addAll(r.resolve(workflowID, toResolve));           
					} catch (Exception ex) {
						Log.logException("Exception at resolving file: " + file ,ex);
					}
				}
			}
		}

		return results;

	}


	/**
	 * Helper methods, mostly copy and pasted from other sources
	 */

	public StorageClient getStorageClientForFileSet(String workflowID, String url_base) throws ProcessingException {
		try{
			String url=getURL(url_base);
			return getSMSClient(url, workflowID);
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

	/**
	 * make a valid URL from the given base (i.e. strip off any leading protocol)
	 * @param base
	 */
	protected static String getURL(String base)throws Exception{
		//check if we have an extra U6 protocol in the string
		int i=base.indexOf(':');
		String sub=base.substring(i+1);
		if ( sub.startsWith("http://") || sub.startsWith("https://")){
			int j=sub.indexOf('#');
			if(j>-1){
				sub=sub.substring(0, j);
			}
			return sub;
		}  
		return base;
	}
	/**
	 * returns a file as a string
	 * @param workflowID 
	 * @param resolved_url URI of the actual grid files
	 * @return Contents of the actual stream
	 */
	public String getContentsOfUrl(String workflowID, String resolved_url){
		
		String output;
		FiletransferClient fts = null;
		try{
			String url = resolved_url;
			String url_without_base;
			String url_base;
			int index = url.lastIndexOf("/files/");
			if(index > 0) { 
				url_without_base = url.substring(index+7);
				url_base = url.substring(0,index);
			} else {
				url_without_base = url;
				url_base = url;
			}
			StorageClient sms = getStorageClientForFileSet(workflowID, url_base);
			logger.debug("Trying to read: " + url_without_base);
			fts = sms.createExport(url_without_base,"BFT",null);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			((FiletransferOptions.Read)fts).readAllData(baos);
			logger.debug("Content of " + resolved_url + " is: " + baos.toString());
			output = baos.toString();
		} catch (Exception ex) {
			logger.error("Exception while retrieving ContentsOfUrl " + ex.getMessage() + " " +ex.toString());
			output = "";
		}
		finally{
			if(fts != null){
				try{
					fts.delete();
				}catch(Exception ex){}
			}
		}
		return output;
	}

	protected StorageClient getSMSClient(String url, String workflowID) throws Exception{
		IClientConfiguration props=PEConfig.getInstance().getKernel().getClientConfiguration();
		return new SMSResolver().getSMSClient(url, workflowID, props);
	}

	/**
	 * chops a String uri into a fileset with one include, supports only c9m and BFT, not local
	 * @param uri URI of the gridfile as string containing either c9m: or BFT: in the front
	 * @param WorkflowID
	 * @return FileSet for the uri
	 */
	public static FileSet reformatStorageURI(String uri, String WorkflowID) {
		String base;
		String[] includes = new String[1];
		String[] excludes = new String[0];
		//currently we assume it is a c9m uri and we only need to replace $WORKFLOW_ID
		String replaced = uri.replaceAll("\\$\\{WORKFLOW_ID\\}",WorkflowID);
		if (replaced.startsWith("c9m:")) {
			int index = replaced.lastIndexOf('/');
			base = replaced.substring(0,index+1);
			includes[0] = replaced.substring(index+1);
		}
		else {
			//The only way to handle all other uris is to cut at the hash
			//Local URIs are not supported
			int index = replaced.lastIndexOf('#');
			base = replaced.substring(0,index+1);
			includes[0] = replaced.substring(index+1);
		}
		FileSet returnvalue = new FileSet(base,includes,excludes,false,false);
		return returnvalue;
	}
	/**
	 * returns only the filename (as in: everything after the final slash)
	 * @param fullPath String of the full Path
	 * @return everything after the final slash.
	 */
	public static String getFileNameOnly(String fullPath){
		while(fullPath.endsWith("/")) fullPath = fullPath.substring(0,fullPath.length()-1);
		int lastSep = fullPath.lastIndexOf("/");
		String name=fullPath;
		if(lastSep > 0)
			name=fullPath.substring(lastSep+1);
		return name;
	}
	
}
