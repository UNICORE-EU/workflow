package eu.unicore.workflow.pe.iterators;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * Default implementation of the {@link Iterate} interface. Iteration values are
 * based on evaluating process variables.
 * 
 * @author schuller
 */
public class Iteration implements Serializable, Iterate{

	private static final Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, Iteration.class);
	
	private static final long serialVersionUID = 1L;
	
	/*
	 * the name of the iteration variable. This is a single variable name,
	 * without "${...}"
	 */
	protected String iteratorName=null;
	
	/*
	 * the resolved value of the iteration variable
	 */
	protected String resolvedIteratorName=null;
	
	/*
	 * the base value of the iteration. This can contain
	 * variables ${...} that are resolved when computing the iteration value
	 * using resolveBase()
	 */
	protected String base=null;
	
	/*
	 * the base value with all variables resolved
	 */
	protected String resolvedBase=null;
	
	
	//this is used to detect if the value has changed
	protected volatile String lastValue=null;
	
	public Iteration(){}

	public void next(final ProcessVariables vars){
		if(base!=null || iteratorName!=null)
			resolve(vars);
			String val=getCurrentValue();
			if(lastValue!=null && val.equals(lastValue)){
				logger.warn("Iteration value did not change! Probably your loop increment expression is faulty?");
			}else{
				lastValue=val;
			}
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
		StringBuilder sb=new StringBuilder();
		if(base!=null){
			if(resolvedBase==null){
				//progammer error
				throw new IllegalStateException("Need to call next() before retrieving value!");
			}
			sb.append(resolvedBase);
		}
		
		if(iteratorName!=null){
			if(resolvedIteratorName==null){
				//progammer error
				throw new IllegalStateException("Can't resolve iterator <"+iteratorName+">");
			}
			if(resolvedBase!=null && resolvedBase.length()>0)sb.append(SEPARATOR);
			sb.append(resolvedIteratorName);
		}
		
		return sb.toString();
	}
	
	public String getBase() {
		return base;
	}
	
	public void setBase(String base) {
		this.base = base;
	}
	
	public String getIteratorName() {
		return iteratorName;
	}
	
	/**
	 * a process variable can be used as the "inner" iterator, instead of the
	 * default counter 
	 * @param iteratorName
	 */
	public void setIteratorName(String iteratorName) {
		this.iteratorName = iteratorName;
	}
	
	/*
	 * resolves variables in the "base" iteration value
	 */
	private static Pattern p=Pattern.compile("\\$\\{\\w*\\}");
	
	protected void resolveBase(ProcessVariables vars){
		resolvedBase=base;
		if(base==null){
			return;
		}
		Matcher m=p.matcher(base);
		while(m.find()){
			String key=m.group().substring(2, m.group().length()-1);
			Object val=vars.get(key);
			String value=val!=null?String.valueOf(vars.get(key)):"";
			resolvedBase=resolvedBase.replace(m.group(), value);
		}
		logger.debug("Resolved base <{}> as <{}>", base, resolvedBase);
	}

	protected void resolveIterator(ProcessVariables vars){
		String value=String.valueOf(vars.get(iteratorName));
		resolvedIteratorName=value;
		logger.debug("Resolved iterator <{}> as <{}>", iteratorName, resolvedIteratorName);
	}

	protected void resolve(final ProcessVariables vars){
		resolveBase(vars);
		if(iteratorName!=null)resolveIterator(vars);
	}
	
	public String getResolvedBase() {
		return resolvedBase;
	}
	
	public String toString(){
		return getCurrentValue();
	}

	/**
	 * this default implementation does nothing
	 */
	public void reset(final ProcessVariables vars)throws IterationException{
	}

	public Iteration clone()throws CloneNotSupportedException{
		return (Iteration)super.clone();
	}
	
	public void fillContext(ProcessVariables vars){
		//NOP
	}
}
