package eu.unicore.workflow.pe.iterators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.workflow.pe.iterators.ResolverFactory.Resolver;
import eu.unicore.xnjs.ems.ProcessingException;

public class TestResolverFactory {

	@BeforeAll
	public static void cleanUp() {
		ResolverFactory.clear();
		ResolverFactory.registerResolver(StorageResolver.class);
		ResolverFactory.registerResolver(WorkflowFileResolver.class);
	}
	
    @Test
	public void testDuplicateResolverRegistration(){
    	ResolverFactory.registerResolver(StorageResolver.class);
		assert 2==ResolverFactory.resolvers.size();
		ResolverFactory.registerResolver(WorkflowFileResolver.class);
		assert 2==ResolverFactory.resolvers.size();
	}

    @Test
	public void testResolveSMS()throws ProcessingException{
		Resolver r=ResolverFactory.getResolver("https://unicore/rest/core/storages/...");
		assert r!=null;
		assert r instanceof StorageResolver;
	}
	
	@Test
	public void testResolverCannotResolveBase()throws Exception{
		 assertThrows(ProcessingException.class, ()->{
			 ResolverFactory.getResolver("this does not exist");
		 });
		}

}
