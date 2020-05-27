package eu.unicore.workflow.pe.persistence;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.chemomentum.dsws.ConversionResult;

import de.fzj.unicore.persist.annotations.ID;
import de.fzj.unicore.persist.annotations.Table;
import de.fzj.unicore.persist.util.JSON;
import de.fzj.unicore.persist.util.Wrapper;
import de.fzj.unicore.xnjs.persistence.GSONUtils;

/**
 * persistent information about a top-level workflow
 * 
 * @author schuller
 */
@Table(name="WORKFLOWS")
@JSON(customHandlers={GSONUtils.XmlBeansConverter.class,Wrapper.WrapperConverter.class})
public class WorkflowContainer extends SubflowContainer{
	
	private static final long serialVersionUID=1;
	
	private String userDN;
	
	private ConversionResult conversionResult;
	
	private String dialect;
	
	private String storageURL;
	
	private Calendar lifetime;

	// maps job IDs to activity IDs
	private final Map<String,String>jobMap = new HashMap<>();
	
	@ID
	@Override
	public String getWorkflowID(){
		return super.getWorkflowID();
	}
	
	/**
	 * get the DN of the user who submitted the workflow
	 */
	public String getUserDN() {
		return userDN;
	}

	public void setUserDN(String userDN) {
		this.userDN = userDN;
	}

	public ConversionResult getConversionResult() {
		return conversionResult;
	}

	public void setConversionResult(ConversionResult conversionResult) {
		this.conversionResult = conversionResult;
	}

	public String getDialect(){
		return dialect;
	}
	
	public void setDialect(String dialect){
		this.dialect=dialect;
	}
	
	public String getStorageURL() {
		return storageURL;
	}

	public void setStorageURL(String storageURL) {
		this.storageURL= storageURL;
	}
	
	
	public Calendar getLifetime() {
		return lifetime;
	}

	public void setLifetime(Calendar lifetime) {
		this.lifetime = lifetime;
	}

	public Map<String,String> getJobMap() {
		return jobMap;
	}
}
