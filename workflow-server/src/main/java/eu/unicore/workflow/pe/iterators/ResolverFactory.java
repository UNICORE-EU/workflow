package eu.unicore.workflow.pe.iterators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import eu.unicore.util.Pair;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.xnjs.ems.ProcessingException;
import eu.unicore.xnjs.util.ErrorCode;

/**
 * Use this to get a {@link Resolver} that can resolve elements of file sets
 *  
 * @author schuller
 */
public class ResolverFactory {

	static final Set<Class<? extends Resolver>> resolvers = new HashSet<>();

	static {
		resolvers.add(StorageResolver.class);
		resolvers.add(WorkflowFileResolver.class);
	}

	public static synchronized void clear(){
		resolvers.clear();
	};

	public static synchronized void reset(){
		resolvers.clear();
		resolvers.add(StorageResolver.class);
		resolvers.add(WorkflowFileResolver.class);
	};
	
	public static synchronized void registerResolver(Class<? extends Resolver> resolver){
		resolvers.add(resolver);
	};

	/**
	 * creates a new resolver instance for the given base
	 * @param base
	 * @throws ProcessingException
	 */
	public static Resolver getResolver(String base)throws ProcessingException{
		for(Class<? extends Resolver> c: resolvers){
			try{
				Resolver r = c.getConstructor().newInstance();
				if(r.acceptBase(base)){
					return r;
				}
			}catch(Exception iae){
				throw new ProcessingException(new ErrorCode(0,"Cannot create resolver for <"+base+">"));
			}
		}
		throw new ProcessingException(new ErrorCode(0,"No matching resolver found for <"+base+">"));
	}

	public static interface Resolver{
		public boolean acceptBase(String base);

		/**
		 * find files matching the given fileset 
		 * @param workflowID
		 * @param fileset
		 * @return a collection of pairs (filename,size) where size is -1 if unknown
		 * @throws ProcessingException
		 */
		public Collection<Pair<String, Long>> resolve(String workflowID, FileSet fileset)throws ProcessingException;
	}

}
