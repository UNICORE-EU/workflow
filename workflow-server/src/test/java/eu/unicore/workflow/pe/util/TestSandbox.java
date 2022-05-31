package eu.unicore.workflow.pe.util;

import groovy.lang.GroovyShell;

import java.security.AccessControlException;

import org.junit.Test;

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

}
