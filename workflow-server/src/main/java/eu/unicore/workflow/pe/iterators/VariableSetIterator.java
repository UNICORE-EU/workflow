package eu.unicore.workflow.pe.iterators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.VariableConstants;
import eu.unicore.workflow.pe.model.EvaluationException;
import eu.unicore.workflow.pe.model.util.VariableUtil;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.util.ScriptEvaluator;

/**
 * iterator over a set of values calculated from
 * variable expressions
 *  
 * @author schuller
 */
public class VariableSetIterator extends ValueSetIterator {

	private static final long serialVersionUID = 1L;

	private final static Logger logger = Log.getLogger(WorkflowProperties.LOG_CATEGORY, VariableSetIterator.class);
	
	private final VariableSet[] variableSets;
	
	/**
	 * @param varSets - the variable sets
	 */
	public VariableSetIterator(VariableSet... varSets){
		this.variableSets=varSets;
	}
	
	/**
	 * (re-)initialise the list of values from the defined variable sets
	 * @param vars - ProcessVariables
	 * @throws ExecutionException
	 */
	protected void reInit(final ProcessVariables vars)throws ExecutionException{
		List<String>results = new ArrayList<>();
		try{
			zip(results,vars.copy(),null,variableSets);
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
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
			List<String>values = new ArrayList<>();
			Map<String, Object> vars = ctx.asMap();
			vars.put(variableName, VariableUtil.create(variableType,startValue));
			String nextValue=startValue;
			int c=0;
			try{
				while(true){
					Boolean cont = (Boolean)ScriptEvaluator.evaluate(condition, vars);
					if(cont==null || !cont)break;
					values.add(nextValue);
					ScriptEvaluator.evaluate(modifier, vars);
					Object next = vars.get(variableName);
					vars.put(variableName, next);
					if(nextValue.equals(String.valueOf(next))){
						throw new ExecutionException(0,"Evaluation error: variable value did not change in loop iteration.");
					}
					nextValue=String.valueOf(next);
					c++;
					if(c>1000){
						throw new EvaluationException("Too many values ("+c+") in value set for variable <"+variableName+">");
					}
				}
				return values;
			}catch(Exception ex){
				throw new EvaluationException("Error building value set for variable <"+variableName+">",ex);
			}
		}
	}
}
