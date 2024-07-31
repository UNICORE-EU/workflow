package eu.unicore.workflow.rest;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import eu.unicore.uas.UAS;
import eu.unicore.services.Kernel;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.iterators.ResolverFactory;
import eu.unicore.workflow.pe.xnjs.XNJSProcessEngine;
import eu.unicore.xnjs.XNJS;

/**
 * starts a full USE workflow engine
 */
public abstract class WSSTestBase {

	protected static Kernel kernel;

	protected static UAS uas;
	
	@BeforeEach
	public void cleanup()throws Exception{
		FileUtils.deleteQuietly(new File("target/data/WORK"));
		FileUtils.forceMkdir(new File("target/data/WORK"));
		ResolverFactory.reset();
	}
	
	@BeforeAll
	public static void setUp()throws Exception{
		FileUtils.deleteQuietly(new File("target/data"));
		uas=new UAS("src/test/resources/container.properties");
		kernel=uas.getKernel();
		uas.startSynchronous();
	}
	
	@AfterAll
	public static void tearDown()throws Exception{
		if(kernel!=null)kernel.shutdown();
	}
	
	/**
	 * returns the server port
	 */
	protected int getPort(){
		return 64433;
	}
	
	protected XNJS getXNJS() {
		return ((XNJSProcessEngine)PEConfig.getInstance().getProcessEngine()).getXNJS();
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
