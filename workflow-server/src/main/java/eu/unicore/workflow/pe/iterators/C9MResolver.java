package eu.unicore.workflow.pe.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.client.core.StorageClient;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.Constants;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;

/**
 * this class can resolve logical filenames
 * using the location manager.<br/>
 *
 * This class is not threadsafe.
 * 
 * @author schuller
 */
public class C9MResolver extends SMSResolver {

	private static final Logger logger=Log.getLogger(Log.SERVICES, C9MResolver.class);

	@Override
	public boolean acceptBase(String base) {
		return base!=null && base.startsWith(Constants.LOGICAL_FILENAME_PREFIX);
	}

	@Override
	public Collection<Pair<String,Long>> resolve(String workflowID, FileSet fileset) throws ProcessingException {
		try{
			Kernel kernel=PEConfig.getInstance().getKernel();
			ArrayList<Pair<String,Long>>results=new ArrayList<Pair<String,Long>>();
			results.addAll(getMatches(workflowID, fileset, kernel));
			return results;
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

	protected List<Pair<String,Long>>getMatches(String workflowID, FileSet fileSet, Kernel kernel) throws Exception{
		ArrayList<Pair<String,Long>>results=new ArrayList<Pair<String,Long>>();
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
						smsClient = getSMSClient(getURL(physicalLocation), workflowID, getClientConfiguration(kernel, workflowID));
						if(logger.isDebugEnabled() && smsClient!=null){
							logger.debug("Retrieving file sizes from "+smsClient.getEndpoint().getUrl());
						}
					}
					results.add(new Pair<String,Long>(physicalLocation,getFileSize(smsClient, physicalLocation)));
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

	private IClientConfiguration clientConfig = null;

	protected IClientConfiguration getClientConfiguration(Kernel kernel, String workflowID) throws PersistenceException{
		if(clientConfig==null){
			clientConfig=kernel.getClientConfiguration().clone();
		}
		return clientConfig;
	}

	public boolean equals(Object other){
		return (other!=null && other instanceof C9MResolver);
	}
}
