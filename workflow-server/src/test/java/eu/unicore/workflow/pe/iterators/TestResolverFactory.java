package eu.unicore.workflow.pe.iterators;

import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.workflow.pe.iterators.ResolverFactory.Resolver;

public class TestResolverFactory {

	@BeforeClass
	public static void cleanUp() {
		ResolverFactory.clear();
		ResolverFactory.registerResolver(SMSResolver.class);
		ResolverFactory.registerResolver(C9MResolver.class);
	}
	
    @Test
	public void testDuplicateResolverRegistration(){
    	ResolverFactory.registerResolver(SMSResolver.class);
		assert 2==ResolverFactory.resolvers.size();
		ResolverFactory.registerResolver(C9MResolver.class);
		assert 2==ResolverFactory.resolvers.size();
	}

    @Test
	public void testResolveSMS()throws ProcessingException{
		Resolver r=ResolverFactory.getResolver("https://unicore/rest/core/storages/...");
		assert r!=null;
		assert r instanceof SMSResolver;
	}
	
	@Test(expected=ProcessingException.class)
	public void testResolverCannotResolveBase()throws Exception{
		ResolverFactory.getResolver("this does not exist");
	}

}
