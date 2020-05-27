package eu.unicore.workflow.pe.model;

import java.io.Serializable;

import eu.unicore.workflow.pe.iterators.IterationException;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * Deal with iteration values.<br/> 
 * Since workflow elements can be executed multiple 
 * times, it is necessary to disambiguate them. <br/>
 * {@link Iterate} instances are <em>not threadsafe</em>.
 * 
 * @author schuller
 */
public interface Iterate extends Cloneable, Serializable{
	
	/**
	 * separates the various levels of the iterator value
	 */
	public static final String SEPARATOR=":::";
	
	/**
	 * check if there are more possible values for this iteration
	 */
	public boolean hasNext();

	/**
	 * trigger the next iteration
	 * 
	 * @param vars - the process variables
	 */
	public void next(ProcessVariables vars);

	/**
	 * reset the "innermost" iteration (if applicable)
	 */
	public void reset(final ProcessVariables vars)throws IterationException;

	/**
	 * get the full, resolved value of the iteration
	 */
	public String getCurrentValue();
	
	/**
	 * set the base of this iteration, i.e. the prefix that is used
	 * @param base
	 */
	public void setBase(String base);
	
	/**
	 * put all relevant values into the processing context
	 * @param vars
	 */
	public void fillContext(ProcessVariables vars);
	
	/**
	 * clone this {@link Iterate}
	 * @throws CloneNotSupportedException
	 */
	public Iterate clone()throws CloneNotSupportedException;
	
}