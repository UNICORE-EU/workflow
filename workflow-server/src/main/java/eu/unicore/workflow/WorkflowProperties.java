package eu.unicore.workflow;

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

	private static final Logger logger=Log.getLogger(Log.SERVICES, WorkflowProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX="workflow.";

	/**
	 * XNJS config file
	 */
	public static final String XNJS_CONFIG="xnjsConfiguration";


	/**
	 * property key for controlling tracing ("true" by default)  
	 */
	public static final String TRACING="tracing";
	

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
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
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
		
		// old stuff - to be removed TODO
		META.put("tracing", new PropertyMD("false").setBoolean().setDeprecated().
				setDescription("(deprecated)"));
		META.put(XNJS_CONFIG, new PropertyMD("n/a").
				setDescription("(deprecated)"));
	}

	public WorkflowProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public WorkflowProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}

	public boolean isTracing(){
		return getBooleanValue(TRACING);
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
}
