package eu.unicore.workflow.xnjs;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.chemomentum.dsws.ConversionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.H2Persist;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.features.WorkflowStartupTask;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.files.Locations;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.xnjs.NullECM;
import eu.unicore.workflow.pe.xnjs.XNJSProcessEngine;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.persistence.JDBCActionStoreFactory;

/**
 * starts an XNJS for workflow processing
 */
public abstract class TestBase {

	protected static XNJS xnjs;

	protected static WorkflowProperties workflowProperties;
	
	@BeforeAll
	public static void startUp()throws Exception{
		FileUtils.deleteQuietly(new File("target","data"));
		
		Properties p = new Properties();
		p.put("persistence.directory", "target/data");
		p.put("XNJS.numberofworkers", "2");
		
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().putAll(p);
		cs.addModule(new WFTestModule(cs.getProperties()));
		
		xnjs = new XNJS(cs);
		WorkflowStartupTask.configureProcessing(xnjs);
		xnjs.setProcessingChain("TESTING", null, new String[]
				{"eu.unicore.workflow.pe.xnjs.TestingActivityProcessor"});
		
		xnjs.start();
		
		//setup persistence
		H2Persist<WorkflowContainer>persist = new H2Persist<>();
		persist.setDaoClass(WorkflowContainer.class);
		Properties props=new Properties();
		props.setProperty("persistence.directory", "target/data");
		persist.setConfigSource(new PersistenceProperties(props));
		persist.init();
		PEConfig.getInstance().setPersistence(persist);
		H2Persist<Locations>persist2 = new H2Persist<Locations>();
		persist2.setDaoClass(Locations.class);
		persist2.setConfigSource(new PersistenceProperties(props));
		persist2.init();
		PEConfig.getInstance().setLocationStore(persist2);
		
		
		PEConfig.getInstance().setProcessEngine(new XNJSProcessEngine(xnjs));
	}
	
	public static class WFTestModule extends AbstractModule {
		
		protected final Properties properties;
		
		protected final WorkflowProperties workflowProperties;
		
		public WFTestModule(Properties properties){
			this.properties = properties;
			this.workflowProperties = new WorkflowProperties(properties);
		}

		@Provides
		public WorkflowProperties getWorkflowProperties(){
			return workflowProperties;
		}
		
		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
			bind(IExecutionContextManager.class).to(NullECM.class);
			bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
		}

	}
	
	@AfterAll
	public static void tearDown()throws Exception{
		xnjs.stop();
		PEConfig.getInstance().setProcessEngine(null);
	}


	protected void doProcess(PEWorkflow wf)throws Exception{
		doProcess(wf,null);
	}
	
	protected void doProcess(PEWorkflow wf, String monitoredActivityID)throws Exception{
		String wfID=wf.getID();
		PEConfig.getInstance().getProcessEngine().process(wf, null);
		while(true){
			Integer s=xnjs.get(Manager.class).getStatus(wfID, null);
			if(ActionStatus.DONE!=s.intValue()){
				Thread.sleep(500);
				if(monitoredActivityID!=null){
					System.out.println("Status of <"+monitoredActivityID+">: "+getStatus(wfID,monitoredActivityID));
				}
			}
			else break;
		}
	}

	/**
	 * get the status of an activity (recurses into sub-flows!)
	 * @param workflowID - the workflow ID
	 * @param activityID - the activity ID
	 */
	protected List<PEStatus> getStatus(String workflowID, String activityID) throws Exception {
		WorkflowContainer workflowInfo=PEConfig.getInstance().getPersistence().read(workflowID);
		return workflowInfo.getActivityStatus(activityID,true);
	}
	
	public void waitForDone(String wfID) throws Exception {
		int c=0;
		int s = 0;
		while(c<120){
			s = xnjs.get(Manager.class).getStatus(wfID, null);
			if(ActionStatus.DONE!=s){
				Thread.sleep(1000);
				c++;
			}
			else break;
		}
		assert s == ActionStatus.DONE;
	}
	
	
	protected void printErrors(ConversionResult res){
		if(res.hasConversionErrors()){
			System.out.println("ERRORS:");
			for(String err:res.getConversionErrors()){
				System.out.println(err+"\n");
			}
		}
	}
}
