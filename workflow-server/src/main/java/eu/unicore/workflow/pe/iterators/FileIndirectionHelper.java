package eu.unicore.workflow.pe.iterators;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.util.LimitedByteArrayOutputStream;
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


	public Collection<Pair<String,Long>> resolve() throws Exception {
		Collection<Pair<String,Long>> results = new ArrayList<>();

		// iterate over the given source files which contain the real file names
		Iterator<Pair<String,Long>>sourceIterator=source.iterator();
		while(sourceIterator.hasNext()){
			Pair<String,Long>currentVal=sourceIterator.next();
			String currentFile=currentVal.getM1();
			currentOpenFile = getContentsOfUrl(workflowID,currentFile);
			currentFileScanner = new Scanner(currentOpenFile);

			// now iterate over the contents 
			while(currentFileScanner.hasNext()) {
				String iterLine = currentFileScanner.nextLine();
				String[] files = iterLine.split(" ");
				for (String file : files) {
					FileSet toResolve = reformatStorageURI(file);
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

	public StorageClient getStorageClientForFileSet(String workflowID, String url_base) throws ProcessingException {
		try{
			return getSMSClient(getURL(url_base), workflowID);
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
	 * @param resolved_url URI of the remote file
	 * @return file content
	 */
	public String getContentsOfUrl(String workflowID, String resolved_url) throws Exception {
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
		FiletransferClient fts = null;
		try{
			fts = sms.createExport(url_without_base,"BFT",null);
			ByteArrayOutputStream baos = new LimitedByteArrayOutputStream(1024*1024);
			((FiletransferOptions.Read)fts).readAllData(baos);
			return baos.toString("UTF-8");
		} finally{
			try{
				fts.delete();
			}catch(Exception ex){}
		}
	}

	protected StorageClient getSMSClient(String url, String workflowID) throws Exception{
		IClientConfiguration props=PEConfig.getInstance().getKernel().getClientConfiguration();
		return new SMSResolver().getSMSClient(url, workflowID, props);
	}

	/**
	 * chops a remote file URI into a fileset with one include
	 */
	public static FileSet reformatStorageURI(String uri) {
		String base;
		int index = uri.lastIndexOf('/');
		base = uri.substring(0, index+1);
		String[] includes = new String[] { uri.substring(index+1) };
		return new FileSet(base, includes, new String[0], false, false);
	}

}
