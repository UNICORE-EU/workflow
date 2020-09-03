package eu.unicore.workflow.rest;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.chemomentum.dsws.util.SetupWorkflowService;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.codahale.metrics.MetricRegistry;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * starts a full USE workflow engine
 */
public abstract class WSSTestBase {

	protected static Kernel kernel;

	protected static UAS uas;
	
	@BeforeClass
	public static void setUp()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		
		uas=new UAS("src/test/resources/container.properties");
		kernel=uas.getKernel();
		uas.startSynchronous();
		
		new SetupWorkflowService(kernel, new MetricRegistry()).run();
	}
	
	@AfterClass
	public static void tearDown()throws Exception{
		System.out.println("*** SHUTDOWN");
		if(kernel!=null)kernel.shutdown();
	}
	
	/**
	 * returns the server port
	 */
	protected int getPort(){
		return 64433;
	}
	
	/**
	 * override to provide non-trivial client side security properties
	 * @return
	 */
	protected IClientConfiguration getClientSideSecurityProperties(){
		return new DefaultClientConfiguration() ;
	}
	
	protected String getBaseurl(){
		return kernel.getContainerProperties().getBaseUrl();
	}
	
	protected JSONObject loadJSON(String fileName)throws Exception{
		return new JSONObject(FileUtils.readFileToString(new File(fileName), "UTF-8"));
	}

}
