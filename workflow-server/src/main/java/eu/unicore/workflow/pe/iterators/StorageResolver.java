package eu.unicore.workflow.pe.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.services.Kernel;
import eu.unicore.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.workflow.pe.iterators.ResolverFactory.Resolver;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * resolve files on a UNICORE storage.
 *
 * @author schuller
 */
public class StorageResolver implements Resolver{

	// TODO how to specify the preferred protocol ?
	// private String protocol = "BFT";
	
	@Override
	public boolean acceptBase(String base) {
		return base!=null && base.contains("/rest/core/storages/");
	}

	@Override
	public Collection<Pair<String,Long>> resolve(String workflowID, FileSet fileset) throws ExecutionException {
		try{
			String url=extractStorageURL(fileset.base);
			String baseDir=extractBaseDir(fileset.base);
			List<Pair<String,Long>>results = new ArrayList<>();
			Kernel kernel=PEConfig.getInstance().getKernel();
			StorageClient sms = getSMSClient(url, workflowID, kernel.getClientConfiguration());
			results.addAll(getMatches(sms,baseDir,fileset));
			return results;
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}
	}
	
	protected Collection<Pair<String,Long>>getMatches(StorageClient sms, String base, FileSet fileSet)throws Exception{
		String urlBase = //protocol+":"+
				sms.getEndpoint().getUrl()+"/files";
		List<Pair<String,Long>>result=new ArrayList<>();
		FileList files = sms.ls(base);
		for (FileListEntry file : files.list(0, 1000)) {
			String name = file.path;
			if(file.isDirectory && fileSet.recurse){
				result.addAll(getMatches(sms, name, fileSet)); 
			}
			else{
				if(matches(name,fileSet)){
					Long size=Long.valueOf(file.size);
					result.add(new Pair<>(urlBase+name,size));
				}
			}
		}
		return result;
	}
	
	/**
	 * check if the given path is to be included according to the given fileset
	 * @param path
	 * @param fileSet
	 */
	protected boolean matches(String path, FileSet fileSet){
		boolean included=isIncluded(path, fileSet);
		boolean excluded=isExcluded(path, fileSet);
		return included && !excluded;
	}
	
	protected boolean isIncluded(String path, FileSet fileSet){
		boolean res=false;
		//check if it is in the includes
		if(fileSet.includes.length>0){
			for(String include: fileSet.includes){
				res=res || match(path,include);
			}
		}
		//else everything is included
		else res=true;
		return res;
	}
	
	protected boolean isExcluded(String path, FileSet fileSet){
		if(fileSet.excludes.length>0){
			for(String exclude: fileSet.excludes){
				if(match(path,exclude))return true;
			}
		}
		return false; 
	}
	
	protected boolean match(String path, String expr){
		Pattern p = getPattern(expr);
		boolean res=p.matcher(path).find();
		return res;
	}
	
	private Map<String, Pattern>patterns = new HashMap<>();
	
	Pattern getPattern(String expr){
		Pattern p=patterns.get(expr);
		if(p==null){
			p=compilePattern(expr);
			patterns.put(expr, p);
		}
		return p;
	}
	
	
	
	/*
	 * translate wildcards "*" and "?" into a regular expression pattern
	 * and create the regexp Pattern
	 *
	 * TODO handle special characters?
	 */
	private Pattern compilePattern(String expr){
		StringBuilder pattern=new StringBuilder();
		pattern.append(expr.replace(".","\\.").replace("*", "[^/]*").replace("?", "."));
		pattern.append("\\Z");
		return Pattern.compile(pattern.toString());
	}
	
	/**
	 * extract the storage URL from the given base, i.e. strip off any 
	 * leading protocol and file path
	 * 
	 * @param base
	 */
	String extractStorageURL(String base)throws Exception {
		
		// strip off UNICORE protocol
		int i=base.indexOf(':');
		String sub=base.substring(i+1);
		if ( sub.startsWith("http://") || sub.startsWith("https://")){
			base = sub;
		}
		
		// strip off file path
		int j=base.indexOf("/files/");
		if(j>-1){
			return base.substring(0, j);
		}
		 
		return base;
	}
	
	/**
	 * @return the base dir, starting with "/"
	 */
	String extractBaseDir(String base)throws Exception {
		if(!base.startsWith("/"))base="/"+base;
		int i=base.indexOf("/files/");
		if(i>-1){
			String res=base.substring(i+7);
			if(!res.startsWith("/"))res="/"+res;
			return res;
		}  
		return "/";
	}

	public StorageClient getSMSClient(String url, String workflowID, IClientConfiguration sp)throws Exception{
		String user = readWF(workflowID).getUserDN();
		return getStorageClient(url, sp, user);
	}
	
	public StorageClient getStorageClient(String url, IClientConfiguration sp, String user)throws Exception{
		return new StorageClient(new Endpoint(url), sp, PEConfig.getInstance().getAuthCallback(user));
	}
	
	public int hashCode(){
		return 1;
	}
	
	public boolean equals(Object other){
		return (other!=null && other instanceof StorageResolver);
	}
		
	protected WorkflowContainer readWF(String wfID) throws Exception {
		return PEConfig.getInstance().getPersistence().read(wfID);
	}
}
