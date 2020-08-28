package eu.unicore.workflow.pe.iterators;

import java.io.Serializable;

import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * counter-based implementation of the {@link Iterate} interface
 * 
 * @author schuller
 */
public class CounterIteration implements Serializable, Iterate{

	private static final long serialVersionUID = 1L;
	
	protected Integer value = 0;
	
	public CounterIteration(){}

	public void next(final ProcessVariables vars){
		value++;
	}
	
	/**
	 * check if a next value exists. This implementation always returns true.
	 */
	public boolean hasNext(){
		return true;
	}

	/**
	 * get the full, resolved value of the iteration.
	 * This consists of the resolved value of the "base" parameter,
	 * the resolved iterator and the default counter. 
	 */
	public String getCurrentValue(){
		return String.valueOf(value);
	}
	
	public String getBase() {
		return null;
	}
	
	public void setBase(String base) {
		//nop
	}
	
	public String getIteratorName() {
		return null;
	}
	
	public void setIteratorName(String iteratorName) {
		//nop
	}
	
	public String getResolvedBase() {
		return null;
	}
	
	public String toString(){
		return getCurrentValue();
	}

	public void reset(final ProcessVariables vars)throws IterationException{
		//nop
	}

	public CounterIteration clone()throws CloneNotSupportedException{
		return (CounterIteration)super.clone();
	}
	
	public void fillContext(ProcessVariables vars){
		//NOP
	}
}
