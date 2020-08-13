package eu.unicore.workflow.pe.util;

import groovy.lang.GroovyShell;

import java.security.AccessControlException;

import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;

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
		sb.append("org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument.Factory.newInstance();");
		String script=sb.toString();
		GroovyShell shell=new GroovyShell(CurrentTimeDocument.class.getClassLoader());
		new ScriptSandbox().eval(shell, script);
	}
	
	
	
}
