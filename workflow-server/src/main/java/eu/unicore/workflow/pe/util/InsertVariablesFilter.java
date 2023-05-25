package eu.unicore.workflow.pe.util;

import java.text.MessageFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.workflow.pe.iterators.ForEachFileIterator;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * this filter inserts variables from the workflow context 
 * into the JSON job definition 
 * 
 * @author schuller
 */
public class InsertVariablesFilter {

	private final ProcessVariables context;

	public InsertVariablesFilter(ProcessVariables context){
		this.context=context;
	}

	/**
	 * 1) extract inputs where source is the workflow context and target
	 * the environment. Create environment variables from these. <br/>
	 * 2) replace occurences of "${VARIABLENAME}" in job
	 */
	public JSONObject filter(JSONObject wa) {
		try{
			JSONObject job = new JSONObject(wa.toString());
			Boolean chunked = (Boolean)context.get(ForEachFileIterator.PV_IS_CHUNKED);
			if(chunked!=null && chunked){
				handleChunkedInputs(job);
			}
			return replaceVariables(job);
		}catch(Exception e){
			return wa;
		}
	}

	/**
	 * replace occurrences of ${VARNAME} in the job
	 */
	private JSONObject replaceVariables(JSONObject job) throws Exception{
		String orig = job.toString();
		return new JSONObject(expandVariables(orig));
		
	}

	/**
	 * all stage-ins containing the loop iterator variable are multiplied and modified
	 * so that a chunk of files is staged in
	 */
	private void handleChunkedInputs(JSONObject job)throws Exception{
		JSONArray inputs = job.optJSONArray("Imports");
		if(inputs==null || inputs.length()==0)return;
		
		Integer thisChunkSize = context.get(ForEachFileIterator.PV_THIS_CHUNK_SIZE, Integer.class);
		String iteratorName = context.get(ForEachFileIterator.PV_ITERATOR_NAME, String.class);
		String key="${"+iteratorName+"_VALUE}";
		String pattern=(String)context.get(ForEachFileIterator.PV_FILENAME_FORMAT);

		JSONArray results = new JSONArray();

		for(int r=0; r<inputs.length(); r++){
			JSONObject dst = inputs.getJSONObject(r);
			if(dst.optString("From", null)==null){
				results.put(dst);
				continue;
			}

			String source = dst.getString("From");
			if(!source.contains(key)){
				results.put(dst);
				continue;
			}

			//else need to clone it several timers
			for(int i=1;i<=thisChunkSize;i++){
				JSONObject clone = new JSONObject(dst.toString());
				String newSource = source.replace(key,
						"${"+ForEachFileIterator.PV_FILENAME+"_"+i+"}");
				String newFileName = getFormattedFilename(dst.getString("To"),pattern,i);
				clone.put("To", newFileName);
				clone.put("From", newSource);
				results.put(clone);
			}

		}
		
		if(results.length()>0){
			job.put("Imports", results);
		}
	}

	private String getFormattedFilename(String filename, String pattern, int index){
		if(filename.contains("*")){
			//simple pattern: replace "*" by the current index
			String formattedIndex=MessageFormat.format("{0,number,0000}", index);
			return filename.replaceAll("\\*", formattedIndex);
		}
		else if(filename.contains("${ORIGINAL_FILENAME}")){
			return filename.replace("${ORIGINAL_FILENAME}","${ORIGINAL_FILENAME"+"_"+index+"}");
		}
		else{
			//use the full pattern
			int lastDot=filename.lastIndexOf(".");
			String name=filename;
			String ext="";
			if(lastDot>-1){
				ext=filename.substring(lastDot);
				name=filename.substring(0, lastDot);
			}
			return MessageFormat.format(pattern, index, name, ext);
		}
	}

	private String expandVariables(String var){
		if(var.contains("${")){
			for(String key: context.keySet()){
				String s="${"+key+"}";
				if(var.contains(s)){
					var=var.replace(s, String.valueOf(context.get(key)));
				}
			}
		}
		return var;
	}
}
