package eu.unicore.workflow.pe.iterators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.util.ErrorCode;
import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.VariableConstants;
import eu.unicore.workflow.pe.model.EvaluationException;
import eu.unicore.workflow.pe.model.util.VariableUtil;
import eu.unicore.workflow.pe.util.ScriptEvaluator;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

/**
 * iterator over a set of values calculated from
 * variable expressions
 *  
 * @author schuller
 */
public class VariableSetIterator extends ValueSetIterator {

	private static final long serialVersionUID = 1L;

	private final static Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, VariableSetIterator.class);
	
	private final String workflowID;
	private final VariableSet[] variableSets;
	
	/**
	 * @param workflowID - the workflow ID
	 * @param varSets - the variable sets
	 */
	public VariableSetIterator(String workflowID, VariableSet... varSets){
		this.workflowID=workflowID;
		this.variableSets=varSets;
	}
	
	/**
	 * (re-)initialise the list of values from the defined variable sets
	 * @param vars - ProcessVariables
	 * @throws ProcessingException
	 */
	protected void reInit(final ProcessVariables vars)throws ProcessingException{
		List<String>results=new ArrayList<String>();
		try{
			zip(results,vars.copy(),null,variableSets);
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}

		values=results.toArray(new String[results.size()]);	
		if(logger.isDebugEnabled()){
			StringBuilder sb=new StringBuilder();
			sb.append("Iterating over the following values:\n");
			for(String val: values){
				sb.append(val+" ");
			}
			logger.debug(sb.toString());
		}
	}
	
	protected void zip(List<String>results,ProcessVariables ctx, String base, VariableSet...varSets)throws EvaluationException{
		VariableSet left=varSets[0];
		
		for(String leftValue: left.values(ctx)){
			String prefix=base!=null?base+"_":"";
			if(varSets.length>1){
				String subBase=base!=null?prefix+leftValue:leftValue;
				ctx.put(left.variableName, VariableUtil.create(left.variableType,leftValue));
				int rem=varSets.length-1;
				VariableSet[]remaining=new VariableSet[rem];
				System.arraycopy(varSets, 1, remaining, 0, rem);
				zip(results, ctx, subBase, remaining);
			}
			else{
				results.add(prefix+leftValue);
			}
		}
	}
	
	@Override
	public void reset(final ProcessVariables vars)throws IterationException{
		super.reset(vars);
		try{
			reInit(vars);
		}catch(Exception ex){
			throw new IterationException(ex);
		}
	}
	
	public VariableSetIterator clone()throws CloneNotSupportedException{
		return (VariableSetIterator)super.clone();
	}
	
	public String getWorkflowID() {
		return workflowID;
	}
	
	@Override
	public void fillContext(ProcessVariables vars){
		super.fillContext(vars);
		String curr=getCurrentUnderlyingValue();
		String[] varValues=curr.split("_");
		for(int i=0;i<variableSets.length;i++){
			vars.put(variableSets[i].variableName,VariableUtil.create(variableSets[i].variableType,varValues[i]));
		}
		
	}
	
	public VariableSet[] getVariableSets(){
		return variableSets;
	}
	
	public static class VariableSet implements Serializable{

		private static final long serialVersionUID = 1L;

		final String variableName;
		final String startValue;
		final String modifier;
		final String condition;
		final String variableType;
		
		
		/**
		 * @param variableName
		 * @param startValue
		 * @param condition
		 * @param modifier
		 */
		public VariableSet(String variableName, String startValue, String condition, String modifier){
			this(variableName,startValue,condition,modifier,VariableConstants.VARIABLE_TYPE_INTEGER);
		}
		
		/**
		 * @param variableName
		 * @param startValue
		 * @param condition
		 * @param modifier
		 * @param variableType
		 */
		public VariableSet(String variableName, String startValue, String condition, String modifier, String variableType){
			this.variableName=variableName;
			this.startValue=startValue;
			this.modifier=modifier;
			this.condition=condition;
			this.variableType=variableType;
		}
		
		public List<String> values(ProcessVariables ctx)throws EvaluationException{
			List<String>values=new ArrayList<String>();
			ProcessVariables vars=ctx.copy();
			vars.put(variableName, VariableUtil.create(variableType,startValue));
			String nextValue=startValue;
			ScriptEvaluator eval=new ScriptEvaluator();
			int c=0;
			try{
				while(true){
					boolean cont=eval.evaluate(condition, vars);
					if(!cont)break;
					values.add(nextValue);
					Object next=eval.evaluate(modifier, variableName, vars);
					vars.put(variableName, next);
					if(nextValue.equals(String.valueOf(next))){
						throw new ProcessingException(new ErrorCode(0,"Evaluation error: variable value did not change in loop iteration."));
					}
					nextValue=String.valueOf(next);
					c++;
					if(c>1000){
							logger.debug("Stopping because of too many values");
							throw new EvaluationException("Too many values ("+c+") in value set for variable <"+variableName+">");
					}
				}
				return values;
			}catch(Exception ex){
				throw new EvaluationException("Error building value set for variable <"+variableName+">",ex);
			}
		}
		
		public String toString(){
			return variableType+" "+variableName+"="+startValue+" ; "+condition+" ; "+modifier;
		}
		
	}
}
