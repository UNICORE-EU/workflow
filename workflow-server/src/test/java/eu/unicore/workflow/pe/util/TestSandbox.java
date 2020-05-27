package eu.unicore.workflow.pe.util;

import groovy.lang.GroovyShell;

import java.security.AccessControlException;

import org.junit.Test;
import org.unigrids.x2006.x04.services.jms.HoldDocument;

public class TestSandbox {

	@Test(expected=AccessControlException.class)
	public void testForbiddenExit(){
		String script="System.out.println(Math.PI);" +
				"System.exit(1024)";
		GroovyShell shell=new GroovyShell();
		try{
			new ScriptSandbox().eval(shell, script);
		}catch(AccessControlException ex){
			System.out.println("As expected: "+ex.getMessage());
			throw ex;
		}
	}
	
	@Test(expected=AccessControlException.class)
	public void testForbiddenPropertyAccess(){
		String script="System.setProperty(\"java.vm.version\",\"xx\");";
		GroovyShell shell=new GroovyShell();
		try{
			new ScriptSandbox().eval(shell, script);
		}catch(AccessControlException ex){
			System.out.println("As expected: "+ex.getMessage());
			throw ex;
		}
	}
	
	
	/**
	 * Load an XMLBeans class from within a Groovy script running in the {@link ScriptSandbox}
	 * (tests fix for SF bug #3151819)
	 */
	@Test
	public void testLoadXMLBeansClassFromScript(){
		StringBuilder sb=new StringBuilder();
		sb.append("org.unigrids.x2006.x04.services.jms.HoldDocument.Factory.newInstance();");
		String script=sb.toString();
		GroovyShell shell=new GroovyShell(HoldDocument.class.getClassLoader());
		new ScriptSandbox().eval(shell, script);
	}
	
	
	
}
