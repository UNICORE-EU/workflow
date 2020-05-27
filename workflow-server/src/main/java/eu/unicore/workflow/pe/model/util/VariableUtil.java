package eu.unicore.workflow.pe.model.util;

import java.io.Serializable;

import eu.unicore.workflow.pe.VariableConstants;

public class VariableUtil {

	private VariableUtil(){}
	
	/**
	 * creates a new variable of the given type and value
	 * @see VariableConstants
	 */
	public static Serializable create(String type, String value){
		if(VariableConstants.VARIABLE_TYPE_BOOLEAN.equalsIgnoreCase(type)){
			return Boolean.parseBoolean(value);
		}
		if(VariableConstants.VARIABLE_TYPE_STRING.equalsIgnoreCase(type)){
			return value;
		}
		if(VariableConstants.VARIABLE_TYPE_INTEGER.equalsIgnoreCase(type)){
			try{
				return Integer.parseInt(value);
			}catch(Exception ex){
				throw new IllegalArgumentException("Illegal value <"+value+"> for INTEGER variable");
			}
		}
		if(VariableConstants.VARIABLE_TYPE_FLOAT.equalsIgnoreCase(type)){
			try{
				return Double.parseDouble(value);
			}catch(Exception ex){
				throw new IllegalArgumentException("Illegal value <"+value+"> for FLOAT variable");
			}
		}
		throw new IllegalArgumentException("Can't create variable of type <"+type+">");
	}
	
	public static Serializable update(Object original, String value){
		if(original.getClass().isAssignableFrom(Boolean.class)){
			return Boolean.parseBoolean(value);
		}
		if(original.getClass().isAssignableFrom(String.class)){
			return value;
		}
		if(original.getClass().isAssignableFrom(Integer.class)){
			try{
				return Integer.parseInt(value);
			}catch(Exception ex){
				throw new IllegalArgumentException("Illegal value <"+value+"> for INTEGER variable");
			}
		}
		if(original.getClass().isAssignableFrom(Double.class)){
			try{
				return Double.parseDouble(value);
			}catch(Exception ex){
				throw new IllegalArgumentException("Illegal value <"+value+"> for FLOAT variable");
			}
		}
		throw new IllegalArgumentException("Can't update variable of type <"+original.getClass()+">");
	}
}
