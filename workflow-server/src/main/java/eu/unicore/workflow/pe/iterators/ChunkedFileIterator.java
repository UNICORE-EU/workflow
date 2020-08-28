package eu.unicore.workflow.pe.iterators;

import java.text.MessageFormat;

import eu.unicore.workflow.pe.model.ForEachIterate;
import eu.unicore.workflow.pe.util.ScriptEvaluator;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * allows to process multiple files in each iteration of a for-each loop
 *  
 * @author schuller
 */
public class ChunkedFileIterator extends Iteration implements ForEachIterate {

	private static final long serialVersionUID=1L; 
	
	/**
	 * Process variables flag (=Boolean.TRUE) to indicate that chunking is active
	 */
	public final static String PV_IS_CHUNKED="ChunkedFileIterator_IS_CHUNKED";
	
	/**
	 * how large is the current chunk (an Integer)
	 */
	public final static String PV_THIS_CHUNK_SIZE="ChunkedFileIterator_THIS_CHUNK_SIZE";
	
	/**
	 * what is the size of all the files in the current chunk (Long)
	 */
	public final static String PV_AGGREGATED_CHUNK_SIZE="ChunkedFileIterator_AGGREGATED_FILE_SIZE";
	
	/**
	 * filename
	 */
	public final static String PV_FILENAME="ChunkedFileIterator_FILENAME";
	
	/**
	 * List of file names separated by ";"
	 * This avoids the need to declare large numbers of variables and allows for
	 * the automatic evaluation of the lists within a job. 
	 */
	private static final String PV_ORIGINAL_FILENAMES = "ChunkedFileIterator_FILENAMES";
	
	/**
	 * filename format string
	 */
	public final static String PV_FILENAME_FORMAT="ChunkedFileIterator_FILENAME_FORMAT";
	
	/**
	 * iterator name of the for-each loop
	 */
	public final static String PV_ITERATOR_NAME="ChunkedFileIterator_ITERATOR_NAME";
	
	/**
	 * variable name for the total number of files
	 */
	public final static String PV_TOTAL_NUMBER="TOTAL_NUMBER";
	
	/**
	 * variable name for the total size of all the files
	 */
	public final static String PV_TOTAL_SIZE="TOTAL_SIZE";
	
	private FileSetIterator source;
	
	private int chunkSize=-1;
	
	private String chunkSizeExpression;
	
	/**
	 * if <code>true</code> the chunkSize is interpreted as an actual aggregated file size...
	 */
	private final boolean isActualDataSize;
	
	private int currentChunk=-1;
	
	public static final String DEFAULT_FORMAT="{0,number}_{1}{2}";
	
	private String fileNameFormatString=DEFAULT_FORMAT;
	
	// only used by persistence
	ChunkedFileIterator(){
		this.source=null;
		this.chunkSize=0;
		this.isActualDataSize=true;
	}
	
	/**
	 * Create a new ChunkedFileIterator with the given file set and chunk size
	 *  
	 * @param source -  the underlying file set iterator
	 * @param chunkSize - the chunk size
	 * @throws IllegalArgumentException if chunksize is < 2
	 */
	public ChunkedFileIterator(FileSetIterator source, int chunkSize){
		this(source,chunkSize,false);
	}

	/**
	 * Create a new ChunkedFileIterator with the given file set and chunk size
	 *  
	 * @param source -  the underlying file set iterator
	 * @param chunkSize - the chunk size
	 * @param isAggregatedFileSize - if <code>true</code>, the chunk size is 
	 *        interpreted as aggregated file size in kbytes
	 * @throws IllegalArgumentException if chunksize is < 2
	 */
	public ChunkedFileIterator(FileSetIterator source, int chunkSize, boolean isAggregatedFileSize){
		this.source=source;
		this.chunkSize=chunkSize;
		this.isActualDataSize=isAggregatedFileSize;
		if(chunkSize<2 && !isAggregatedFileSize){
			throw new IllegalArgumentException("Chunk size must be larger than 1!");
		}
	}

	/**
	 * Create a new ChunkedFileIterator with the given file set and 
	 * expression to calculate the chunk size
	 *  
	 * @param source - the underlying file set iterator
	 * @param chunkSizeExpression - a Groovy expression for dynamically calculating the chunk size
	 * @param isAggregatedFileSize - if <code>true</code>, the chunk size is 
	 *        interpreted as aggregated file size in kbytes
	 */
	public ChunkedFileIterator(FileSetIterator source, String chunkSizeExpression, boolean isAggregatedFileSize){
		this.source=source;
		this.chunkSizeExpression=chunkSizeExpression;
		this.isActualDataSize=isAggregatedFileSize;
		if(chunkSizeExpression==null)throw new IllegalArgumentException("Expression cannot be null");
	}
	
	@Override
	public void setIteratorName(String iteratorName) {
		super.setIteratorName(iteratorName);
		source.setIteratorName(iteratorName);
	}


	/**
	 * set the format string for generating the filenames for stage-in.<br/>
	 * By default, the index is pre-fixed, i.e. "filename" is changed
	 * to "1_filename" to "N_filename". This string is used to construct
	 * a {@link MessageFormat} instance, which is then fed with the index,
	 * 
	 */
	public void setFormatString(String formatString){
		this.fileNameFormatString=formatString;
	}

	public String getFormatString(){
		return fileNameFormatString;
	}

	public void fillContext(ProcessVariables vars) {
		//cleanup first
		for(String key: vars.keySet()){
			if(key.startsWith(PV_FILENAME)){
				vars.put(key,null);
			}
		}
		vars.put(PV_IS_CHUNKED, Boolean.TRUE);
		
		if(isActualDataSize){
			fillContextFileSizeLimited(vars);
		}
		else{
			fillContextFileNumberLimited(vars);
		}
		vars.put(PV_ITERATOR_NAME, source.getIteratorName());
		vars.put(PV_FILENAME_FORMAT, fileNameFormatString);
		vars.put(PV_CURRENT_FOR_EACH_INDEX, getCurrentIndex());
		if(source.getIteratorName()!=null){
			vars.put(source.getIteratorName(), getCurrentIndex());
		}
	}

	protected void fillContextFileNumberLimited(ProcessVariables vars){
		int count=0;
		StringBuilder filenames = new StringBuilder();
		for(int i=0;i<chunkSize;i++){
			if(source.hasNext()){
				source.next(vars);
				String currentFile=source.getCurrentUnderlyingValue();
				vars.put(PV_FILENAME+"_"+(i+1), currentFile);
				vars.put("ORIGINAL_FILENAME_"+(i+1), source.getCurrentFileName());
				filenames.append((count == 0 ? "" : ";") + source.getCurrentFileName());
				count++;
			}
			else break;
		}
		vars.put(PV_THIS_CHUNK_SIZE, Integer.valueOf(count));
		vars.put(PV_ORIGINAL_FILENAMES, filenames.toString());
	}
	
	/**
	 * generate a chunk that has either an aggregated size of less than the
	 * chunk size, or contains only a single file
	 * @param vars
	 */
	protected void fillContextFileSizeLimited(ProcessVariables vars){
		int count=0;
		long total=0;
		long limitBytes=chunkSize*1024; //chunkSize is in Kbytes
		final StringBuilder filenames = new StringBuilder();
		while(total<limitBytes){
			if(source.hasNext()){
				Long length=source.peekNextFileSize();
				if(total+length>limitBytes && count>0){
					//end this chunk
					break;
				}
				//add the file to the current chunk
				source.next(vars);
				String currentFile=source.getCurrentUnderlyingValue();
				vars.put(PV_FILENAME+"_"+(count+1), currentFile);
				vars.put("ORIGINAL_FILENAME_"+(count+1), source.getCurrentFileName());
				filenames.append((count==0 ? "" : ";") + source.getCurrentFileName());
				count++;
				if(length>=0){
					total+=length;
				}
				else{
					//size is not known, end the chunk
					break;
				}
			}
			else break;
		}
		vars.put(PV_THIS_CHUNK_SIZE, Integer.valueOf(count));
		vars.put(PV_AGGREGATED_CHUNK_SIZE, Long.valueOf(total));
		vars.put(PV_ORIGINAL_FILENAMES, filenames.toString());
	}
	
	public String getCurrentIndex() {
		return String.valueOf(currentChunk);
	}

	
	public boolean hasNext() {
		return source.hasNext();
	}

	public void next(ProcessVariables vars) {
		super.next(vars);
		currentChunk++;
	}

	public void reset(ProcessVariables vars) throws IterationException {
		currentChunk=-1;
		super.reset(vars);
		source.reset(vars);
		if(chunkSize==-1){
			chunkSize=calculateChunkSize(vars);
		}
	}

	public void setBase(String base) {
		source.setBase(base);
		super.setBase(base);
	}
	
	@Override
	protected void resolveIterator(ProcessVariables vars){
		resolvedIteratorName=String.valueOf(currentChunk+1);
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public FileSetIterator getSource() {
		return source;
	}
	
	public ChunkedFileIterator clone()throws CloneNotSupportedException{
		ChunkedFileIterator cloned=(ChunkedFileIterator)super.clone();
		cloned.source=this.source.clone();
		return cloned;
	}
	
	protected int calculateChunkSize(ProcessVariables varsOrig){
		// use a copy to prevent side effects
		ProcessVariables vars=varsOrig.copy();
		vars.put(PV_TOTAL_NUMBER, source.getTotalNumberOfFiles());
		vars.put(PV_TOTAL_SIZE, source.getTotalFileSize());
		ScriptEvaluator eval=new ScriptEvaluator();
		Object res=eval.evaluateDirect(chunkSizeExpression, vars);
		try{
			return Integer.valueOf(String.valueOf(res));
		}
		catch(Exception ex){
			throw new IllegalArgumentException("Expression does not evaluate to an Integer! " +
					"Result was '"+res+"' of class "+res.getClass());
		}
	}
	
	/**
	 * test the supplied format string
	 * @param format
	 * @throws IllegalArgumentException
	 */
	public static void testFormat(String format){
		MessageFormat.format(format, 100,"test",".txt");
	}

}
