package eu.unicore.workflow.pe.iterators;

import java.text.MessageFormat;
import java.util.Map;

import de.fzj.unicore.xnjs.util.ScriptEvaluator;
import eu.unicore.workflow.pe.model.ForEachIterate;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * Process one or multiple files in each iteration of a for-each loop
 *
 * @author schuller
 */
public class ForEachFileIterator extends Iteration implements ForEachIterate {

	private static final long serialVersionUID=1L; 

	public static enum Type {
		NUMBER, 
		SIZE
	}
	
	/**
	 * Process variables flag (=Boolean.TRUE) to indicate that chunking is active
	 */
	public final static String PV_IS_CHUNKED="__ForEachFileIterator_IS_CHUNKED";

	/**
	 * iterator name of the for-each loop
	 */
	public final static String PV_ITERATOR_NAME="__ForEachFileIterator_ITERATOR_NAME";
	
	/**
	 * how large is the current chunk (an Integer)
	 */
	public final static String PV_THIS_CHUNK_SIZE="_CHUNK_SIZE";
	
	/**
	 * what is the size of all the files in the current chunk (Long)
	 */
	public final static String PV_AGGREGATED_CHUNK_SIZE="__FILE_SIZE";

	/**
	 * Variable holding the filename
	 */
	public final static String PV_FILENAME="_FILENAME";

	/**
	 * Variable holding the file path
	 */
	public final static String PV_VALUE="_VALUE";

	/**
	 * filename format string
	 */
	public final static String PV_FILENAME_FORMAT="__FILENAME_FORMAT";
	
	/**
	 * variable name holding the total number of files
	 */
	public final static String EXPR_TOTAL_NUMBER="TOTAL_NUMBER";
	
	/**
	 * variable name for the total size of all the files
	 */
	public final static String EXPR_TOTAL_SIZE="TOTAL_SIZE";
	
	private FileSetIterator source;
	
	private int chunkSize = -1;
	
	private String chunkSizeExpression;
	
	private final Type type;
	
	private int currentChunk = -1;
	
	public static final String DEFAULT_FORMAT="{0,number}_{1}{2}";
	
	private String fileNameFormatString=DEFAULT_FORMAT;
	
	// only used by persistence
	ForEachFileIterator(){
		this.source = null;
		this.chunkSize = 0;
		this.type = Type.NUMBER;
	}
	
	/**
	 * Create a new ChunkedFileIterator with the given file set and chunk size
	 *  
	 * @param source -  the underlying file set iterator
	 * @param chunkSize - the chunk size
	 * @throws IllegalArgumentException if chunksize is < 2
	 */
	public ForEachFileIterator(FileSetIterator source, int chunkSize){
		this(source, chunkSize, Type.NUMBER);
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
	public ForEachFileIterator(FileSetIterator source, int chunkSize, Type type){
		this.source=source;
		this.chunkSize=chunkSize;
		this.type = type;
		if(chunkSize<1 && Type.NUMBER.equals(type)){
			throw new IllegalArgumentException("Chunk size must be larger than zero!");
		}
	}

	/**
	 * Create a new ForEachFileIterator with the given file set and
	 * expression to calculate the chunk size
	 *  
	 * @param source - the underlying file set iterator
	 * @param chunkSizeExpression - a Groovy expression for dynamically calculating the chunk size
	 * @param type - how the chunk size is interpreted
	 */
	public ForEachFileIterator(FileSetIterator source, String chunkSizeExpression, Type type){
		this.source=source;
		this.chunkSizeExpression=chunkSizeExpression;
		this.type = type;
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
	
	public String getFormatString() {
		return fileNameFormatString;
	}

	public void fillContext(ProcessVariables vars) {
		String iteratorName = getIteratorName();
		//cleanup first
		for(Object key: vars.keySet().toArray()){
			String k = key.toString();
			if(k.startsWith(iteratorName+PV_FILENAME) 
				|| k.startsWith(iteratorName+PV_ORIGINAL_FILENAMES)
				|| k.startsWith(iteratorName+PV_ORIGINAL_FILENAME)
				|| k.startsWith(iteratorName+PV_VALUE)
				){
				vars.remove(k);
			}
		}
		vars.put(PV_IS_CHUNKED, Boolean.TRUE);
		
		if(Type.SIZE.equals(type)){
			fillContextFileSizeLimited(vars);
		}
		else{
			fillContextFileNumberLimited(vars);
		}
		vars.put(PV_ITERATOR_NAME, source.getIteratorName());
		vars.put(PV_FILENAME_FORMAT, fileNameFormatString);
		vars.put(iteratorName+PV_CURRENT_FOR_EACH_INDEX, getCurrentIndex());
		vars.put(source.getIteratorName(), getCurrentIndex());
	}

	protected void fillContextFileNumberLimited(ProcessVariables vars){
		int count=0;
		String iteratorName = getIteratorName();
		StringBuilder filenames = new StringBuilder();
		for(int i=0;i<chunkSize;i++){
			if(source.hasNext()){
				source.next(vars);
				String currentFile=source.getCurrentUnderlyingValue();
				vars.put(iteratorName+PV_FILENAME+"_"+(i+1), currentFile);
				if(chunkSize==1) {
					// add variables without appended "_1" for this case
					vars.put(iteratorName+PV_ORIGINAL_FILENAME,
							source.getCurrentFileName());
					vars.put(getIteratorName()+PV_FILENAME,
							source.getCurrentFileName());
					vars.put(getIteratorName()+PV_VALUE,
							currentFile);
				}
				vars.put(iteratorName+PV_ORIGINAL_FILENAME+"_"+(i+1),
						source.getCurrentFileName());
				
				filenames.append((count == 0 ? "" : ";") + source.getCurrentFileName());
				count++;
			}
			else break;
		}
		vars.put(iteratorName+PV_THIS_CHUNK_SIZE, Integer.valueOf(count));
		vars.put(iteratorName+PV_ORIGINAL_FILENAMES, filenames.toString());
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
		String iteratorName = getIteratorName();
		
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
				vars.put(iteratorName+PV_ORIGINAL_FILENAME+(count+1), source.getCurrentFileName());
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
		vars.put(iteratorName+PV_ORIGINAL_FILENAMES, filenames.toString());
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
	
	protected int calculateChunkSize(ProcessVariables varsOrig){
		Map<String,Object> vars = varsOrig.asMap();
		vars.put(EXPR_TOTAL_NUMBER, source.getTotalNumberOfFiles());
		vars.put(EXPR_TOTAL_SIZE, source.getTotalFileSize());
		Object res = ScriptEvaluator.evaluate(chunkSizeExpression, vars, null);
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
