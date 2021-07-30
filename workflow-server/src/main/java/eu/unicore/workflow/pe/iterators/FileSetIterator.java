package eu.unicore.workflow.pe.iterators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.iterators.ResolverFactory.Resolver;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * iterator to traverse file sets
 *  
 * @author schuller
 */
public class FileSetIterator extends ValueSetIterator {

	private static final long serialVersionUID = 1L;

	private final static Logger logger=Log.getLogger(Log.SERVICES, FileSetIterator.class);
	
	private final String workflowID;
	
	private final FileSet[] fileSets;
	
	private Long[] sizes;
	
	// total size in kbytes
	private long totalSize;
	
	/**
	 * @param workflowID - the workflow ID
	 * @param fileSets - the file sets
	 */
	public FileSetIterator(String workflowID, FileSet... fileSets)throws ProcessingException{
		this.workflowID=workflowID;
		this.fileSets=fileSets;
	}
	
	/**
	 * (re-)initialise the list of values from the defined filesets 
	 * @throws ProcessingException
	 */
	protected void reInit()throws ProcessingException{
		List<Pair<String,Long>>results=new ArrayList<Pair<String,Long>>();
		for(FileSet f: fileSets){
			try{
				Resolver r=ResolverFactory.getResolver(f.base);
				if(f.indirection){
					results.addAll(new FileIndirectionHelper(r.resolve(workflowID, f), workflowID).resolve());
				}
				else{
					results.addAll(r.resolve(workflowID, f));
				}
			}catch(Exception ex){
				throw new ProcessingException(ex);
			}
		}
		fillValuesArray(results);

		logger.info("[{}] Iterating over {} files, total size {} kB.", getWorkflowID(), values.length, totalSize);
		if(logger.isDebugEnabled()){
			StringBuilder sb=new StringBuilder();
			sb.append("Filenames:\n");
			for(String val: values){
				sb.append(val).append('\n');
			}
			logger.debug(sb.toString());
		}
	}
	
	/**
	 * create the final values list.
	 * @param list
	 */
	protected void fillValuesArray(List<Pair<String,Long>> list){
		values=new String[list.size()];
		sizes=new Long[list.size()];
		totalSize=0;
		for(int i=0; i<list.size(); i++){
				values[i]=list.get(i).getM1();
				sizes[i]=list.get(i).getM2();
				totalSize+=sizes[i];
		}
		totalSize=totalSize/1024;
		if(totalSize==0)totalSize=1;
	}

	@Override
	public void reset(ProcessVariables vars)throws IterationException{
		try{
			reInit();
		}catch(Exception ex){
			throw new IterationException(ex);
		}
	}
	
	@Override
	public void fillContext(ProcessVariables vars){
		super.fillContext(vars);
		//add the individual file name
		if(getIteratorName()!=null){
			String filename = getCurrentFileName();
			vars.put(getIteratorName()+"_FILENAME", filename);
		}
	}
	
	/**
	 * gets only the file name (i.e. stripping off the path)
	 * @return the filename part, or null if it is a directory name
	 */
	public String getCurrentFileName(){
		String fullPath = getCurrentUnderlyingValue();
		while(fullPath.endsWith("/")) fullPath = fullPath.substring(0,fullPath.length()-1);
		int lastSep = fullPath.lastIndexOf("/");
		String name=fullPath;
		if(lastSep > 0)
			name=fullPath.substring(lastSep+1);
		
		return name;
	}
	
	/**
	 * extract the file name from a file path
	 * 
	 * @param path - the file path
	 * @return filename or null if filename cannot be determined
	 */
	protected String getFile(String path){
		int i=path.lastIndexOf('/');
		if(i<0 || path.endsWith("/"))return null;
		String res=path.substring(i+1);
		return res;
	}
	
	public Long getCurrentFileSize(){
		return sizes[position];
	}
	
	public Long peekNextFileSize(){
		return sizes[position+1];
	}
	
	public long getTotalFileSize(){
		return totalSize;
	}

	public int getTotalNumberOfFiles(){
		return sizes.length;
	}

	public String getWorkflowID() {
		return workflowID;
	}
	
	public FileSetIterator clone()throws CloneNotSupportedException{
		return (FileSetIterator)super.clone();
	}
	
	public static class FileSet implements Serializable{

		private static final long serialVersionUID = 1L;

		final String base;
		final String[] includes;
		final String[] excludes;
		final boolean recurse;
		final boolean indirection;
		
		/**
		 * @param base - the base path
		 * @param includes - path patterns to include
		 * @param excludes - path patterns to exclude
		 * @param recurse - whether to recurse into subdirs
		 * @param indirection - whether the actual file names should be read FROM the given files
		 */
		public FileSet(String base, String[] includes, String[] excludes, boolean recurse, boolean indirection){
			this.base=base;
			this.includes=includes!=null?includes:new String[]{};
			this.excludes=excludes!=null?excludes:new String[]{};
			this.recurse=recurse;
			this.indirection=indirection;
		}		

	}

}
