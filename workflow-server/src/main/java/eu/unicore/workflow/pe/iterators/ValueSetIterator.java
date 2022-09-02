package eu.unicore.workflow.pe.iterators;

import java.util.NoSuchElementException;

import eu.unicore.workflow.pe.model.ForEachIterate;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * Iterate over a set of string values. Works similarly to a normal Iterator.
 * 
 * @author schuller
 */
public class ValueSetIterator extends Iteration implements ForEachIterate{

	private static final long serialVersionUID = 1L;

	protected String[] values;
	
	protected int position=-1;
	
	public ValueSetIterator(){}
	
	public ValueSetIterator(String ... values){
		this.values=values;
	}
	
	/**
	 * get the current value of the underlying value set
	 */
	public String getCurrentUnderlyingValue(){
		return values[position];
	}
	
	public String getCurrentIndex(){
		return String.valueOf(position);
	}
	
	/**
	 * get the full, resolved value of the iteration
	 */
	public String getCurrentValue(){
		StringBuilder sb=new StringBuilder();
		if(position<0 || base!=null){
			if(resolvedBase==null || position<0){
				//programmer error
				throw new IllegalStateException("Need to call next() before retrieving value!");
			}
			sb.append(resolvedBase);
		}
		
		if(sb.length()>0)sb.append(SEPARATOR);
		sb.append(String.valueOf(position));
		
		return sb.toString();
	}
	
	public boolean hasNext() {
		return values!=null && values.length>0 && position<values.length-1;
	}

	@Override
	public void next(final ProcessVariables vars) {
		if(!hasNext())throw new NoSuchElementException("No more values");
		position++;
		super.next(vars);
	}
	
	public void fillContext(ProcessVariables vars){
		String value=getCurrentUnderlyingValue();
		String index=getCurrentIndex();
		if(getIteratorName() != null){
			vars.put(getIteratorName(), index);
			vars.put(getIteratorName()+"_VALUE", value);
		}
	}

	@Override
	public void reset(final ProcessVariables vars)throws IterationException{
		position=-1;
	}

}
