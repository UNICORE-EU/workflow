package eu.unicore.workflow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration for the Workflow service
 *  
 * @author schuller
 */
public class WorkflowProperties extends PropertiesHelper {

	// base for workflow-related logger categories
	public static final String LOG_CATEGORY = "unicore.workflow.";

	private static final Logger logger = Log.getLogger("unicore.configuration.", WorkflowProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX="workflow.";

	/**
	 * file with additional properties (only loaded for workflow service)
	 */
	public static final String ADDITIONAL_PROPERTIES = "additionalSettings";

	/**
	 * property key for defining the maximum number of activities per activity group  
	 */
	public static final String MAX_ACTIVITIES_PER_GROUP="maxActivitiesPerGroup";
	

	/**
	 * Config property: remove storage when workflow is destroyed (default: false)  
	 */
	public static final String CLEANUP_STORAGES="cleanupStorage";
	
	/**
	 * Config property: remove jobs when workflow is destroyed (default: true)  
	 */
	public static final String CLEANUP_JOBS="cleanupJobs";

	/**
	 * property for defining the maximum number of concurrent activities (hard limit) per for-each group,
	 */
	public static final String FOR_EACH_MAX_CONCURRENT_ACTIVITIES="forEachMaxConcurrentActivities";

	public static final String FOR_EACH_CONCURRENT_ACTIVITIES="forEachConcurrentActivities";

	/**
	 * property key for defining the period for "slow" job status polling  
	 */
	public static final String POLLING="pollingInterval";

	/**
	 * property key for disabling re-submission
	 */
	public static final String DISABLE_RESUBMIT="resubmitDisable";

	/**
	 * property key for controlling maximum number of re-submission
	 */
	public static final String RESUBMIT_LIMIT="resubmitLimit";
	
	public static final int DEFAULT_RESUBMIT_LIMIT=3;
	
	/**
	 * property key for defining the internal mode (workflow service is deployed in a UNICORE/X)
	 */
	public static final String INTERNAL_MODE = "internalMode";

	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	static
	{
		
		META.put(MAX_ACTIVITIES_PER_GROUP, new PropertyMD(String.valueOf("1000")).setInt().setPositive().
				setDescription("Maximum number of workflow activities per activity group."));
		META.put(FOR_EACH_MAX_CONCURRENT_ACTIVITIES, new PropertyMD(String.valueOf("200")).setInt().setPositive().
				setDescription("Hard limit on the number of concurrent for-each iterations."));
		META.put(FOR_EACH_CONCURRENT_ACTIVITIES, new PropertyMD(String.valueOf("100")).setInt().setPositive().
				setDescription("Default maximum number of concurrent for-each iterations (user can increase this)."));
		META.put(CLEANUP_STORAGES, new PropertyMD("false").setBoolean().
				setDescription("Whether to cleanup the workflow storage when the workflow is destroyed."));
		META.put(CLEANUP_JOBS, new PropertyMD("true").setBoolean().
				setDescription("Whether to remove child jobs when the workflow is destroyed."));
		
		META.put(DISABLE_RESUBMIT, new PropertyMD("false").setBoolean().
				setDescription("Whether to disable automatic re-submission of failed jobs."));
		META.put(RESUBMIT_LIMIT, new PropertyMD(String.valueOf(DEFAULT_RESUBMIT_LIMIT)).setInt().setPositive().
				setDescription("Maximum number of re-submissions of failed jobs."));
		META.put(POLLING, new PropertyMD(String.valueOf("600")).setInt().setPositive().
				setDescription("Interval in seconds for (slow) polling of job states."));
		META.put(INTERNAL_MODE, new PropertyMD("false").setBoolean().
				setDescription("Internal mode: Workflow service only uses services deployed in the same UNICORE/X instance."));
		META.put(ADDITIONAL_PROPERTIES, new PropertyMD().setPath().
				setDescription("Optional file containing additional settings (e.g. XNJS.* settings) only used for the workflow service."));
		
		// old stuff - to be removed TODO
		META.put("tracing", new PropertyMD("false").setBoolean().setDeprecated().
				setDescription("(deprecated)"));
		META.put("xnjsConfiguration", new PropertyMD("n/a").
				setDescription("(deprecated)"));
	}

	public WorkflowProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public WorkflowProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, loadAdditionalProperties(properties), META, logger);
	}

	public boolean isInternal(){
		return getBooleanValue(INTERNAL_MODE);
	}
	
	public boolean isStorageCleanup(){
		return getBooleanValue(CLEANUP_STORAGES);
	}
	
	public boolean isJobsCleanup(){
		return getBooleanValue(CLEANUP_JOBS);
	}

	public int getMaxActivitiesPerGroup(){
		return getIntValue(MAX_ACTIVITIES_PER_GROUP);
	}

	public boolean isResubmitDisabled(){
		return getBooleanValue(DISABLE_RESUBMIT);
	}

	public int getResubmissionLimit(){
		return getIntValue(RESUBMIT_LIMIT);
	}
	
	public int getStatusPollingInterval(){
		return getIntValue(POLLING);
	}

	public Properties getRawProperties() {
		return properties;
	}

	private static final String _loaded = "__additional_workflow_properties__";

	private static Properties loadAdditionalProperties(Properties initial) {
		Properties finalProps = new Properties();
		finalProps.putAll(initial);
		String additionalProps = initial.getProperty(PREFIX+ADDITIONAL_PROPERTIES);
		// prevent doing this twice...
		boolean haveAlreadyLoaded = initial.getProperty(_loaded)!=null;
		if(additionalProps!=null && !haveAlreadyLoaded) {
			File f = new File(additionalProps);
			logger.info("Loading additional Workflow system settings from <{}>", f);
			try {
				try(FileReader fr = new FileReader(f)){
					Properties add = new Properties();
					add.load(fr);
					finalProps.putAll(add);
					finalProps.put(_loaded, "true");
				}
			}
			catch(IOException e) {
				throw new ConfigurationException(
						Log.createFaultMessage("Could not load properties from <"
						+additionalProps+">", e));
			}
		}
		return finalProps;
	}
}
