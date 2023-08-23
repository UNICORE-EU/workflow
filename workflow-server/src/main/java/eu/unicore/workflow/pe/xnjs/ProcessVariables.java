package eu.unicore.workflow.pe.xnjs;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.unicore.persist.util.Wrapper;

/**
 * stores workflow variables and keeps track of which variables have
 * been modified during processing
 * 
 * @author schuller
 */
public class ProcessVariables implements Serializable {

	private static final long serialVersionUID = 1L;

	private HashMap<String,Wrapper<Serializable>>entries = new HashMap<>();
	
	private Collection<String>modifiedVariables = new HashSet<>();
	
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T>type){
		Object val=entries.get(key);
		if(val==null)return null;
		val=((Wrapper<Serializable>)val).get();
		if(val.getClass().isAssignableFrom(type)){
			return type.cast(val);
		}
		else throw new IllegalArgumentException("Object found in map, but has wrong type. " +
				"Found <"+val.getClass().getName()+"> expected <"+type.getName()+">");
	}

	public void put(String key, Object value){
		entries.put(key, new Wrapper<Serializable>((Serializable)value));
	}

	public void putAll(ProcessVariables pv){
		for(String k: pv.keySet()){
			put(k,pv.get(k));
		}
	}
	
	public void putAll(Map<? extends String, ? extends Object> m){
		for(Map.Entry<? extends String, ? extends Object>e: m.entrySet()){
			put(e.getKey(),e.getValue());
		}
	}
	
	public void remove(String key){
		entries.remove(key);
	}
	
	public Object get(String key){
		Wrapper<Serializable>e = entries.get(key);
		return e!=null ? e.get() : null;
	}
	
	public Set<String>keySet(){
		return entries.keySet();
	}
	
	public int size(){
		return entries.size();
	}
	
	public Collection<String>getModifiedVariableNames(){
		return modifiedVariables;
	}
	
	public void markModified(String variableName){
		modifiedVariables.add(variableName);
	}
	
	@SuppressWarnings("unchecked")
	public ProcessVariables copy(){
		ProcessVariables copy = new ProcessVariables();
		copy.entries = (HashMap<String,Wrapper<Serializable>>)this.entries.clone();
		return copy;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		for(Map.Entry<String, Wrapper<Serializable>> e: entries.entrySet()) {
			if(sb.length()>2)sb.append("\n");
			String val = e.getValue()!=null? String.valueOf(e.getValue().get()): "<null>";
			sb.append(e.getKey()).append(": ").append(val);
		}
		sb.append("\n}");
		return sb.toString();
	}
	
	public Map<String,Object> asMap(){
		Map<String,Object> vars = new HashMap<>();
		for(String key: entries.keySet()) {
			vars.put(key, get(key));
		}
		return vars;
	}
}
