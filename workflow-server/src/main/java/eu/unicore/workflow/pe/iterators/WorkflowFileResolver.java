package eu.unicore.workflow.pe.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.client.core.StorageClient;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.workflow.Constants;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * this class can resolve "wf:..." file names using the file catalog
 * 
 * @author schuller
 */
public class WorkflowFileResolver extends StorageResolver {

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, WorkflowFileResolver.class);

	@Override
	public boolean acceptBase(String base) {
		return base!=null && base.startsWith(Constants.LOGICAL_FILENAME_PREFIX);
	}

	@Override
	public Collection<Pair<String,Long>> resolve(String workflowID, FileSet fileset) throws ExecutionException {
		try{
			Kernel kernel=PEConfig.getInstance().getKernel();
			ArrayList<Pair<String,Long>>results = new ArrayList<>();
			results.addAll(getMatches(workflowID, fileset, kernel));
			return results;
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}
	}

	protected List<Pair<String,Long>>getMatches(String workflowID, FileSet fileSet, Kernel kernel) throws Exception{
		ArrayList<Pair<String,Long>>results = new ArrayList<>();
		Locations locations = PEConfig.getInstance().getLocationStore().read(workflowID);

		//list files and check includes/excludes
		String[] incl=null;
		if(fileSet.includes.length==0){
			//everything is included
			incl=new String[]{"*"};
		}
		else{
			incl=fileSet.includes;
		}
		for(String include: incl){

			String loc = fileSet.base+include;

			StorageClient smsClient=null;

			for(String logicalName: locations.getLocations().keySet()){
				if(match(logicalName, loc) && !isExcluded(logicalName, fileSet)){
					String physicalLocation = locations.getLocations().get(logicalName);
					if(smsClient==null || !physicalLocation.contains(smsClient.getEndpoint().getUrl())){
						smsClient = getSMSClient(extractStorageURL(physicalLocation),
								workflowID, kernel.getClientConfiguration());
					}
					long size = getFileSize(smsClient, physicalLocation);
					if(size>-1)results.add(new Pair<>(physicalLocation, size));
				}
			}	
		}

		return results;
	}

	protected Long getFileSize(StorageClient smsClient, String physicalLocation){
		try{
			int index = physicalLocation.indexOf("/files/");
			if(index>0){
				String path=physicalLocation.substring(index+7);
				return smsClient.stat(path).size;
			}
		}
		catch(Exception ex){
			logger.error("Cannot retrieve file size",ex);
		}
		return Long.valueOf(-1);
	}

	public boolean equals(Object other){
		return (other!=null && other instanceof WorkflowFileResolver);
	}
}
