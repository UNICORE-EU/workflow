package eu.unicore.workflow.pe.xnjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * helper class for checking workflow execution in unit tests
 * 
 * @author schuller
 */
public class Validate {

	private static final List<String>invoked=new ArrayList<String>();
	private static final Map<String,Integer>invocations=new ConcurrentHashMap<String,Integer>();
	
	private static final List<String>actionIDs=new ArrayList<String>();
	
	private Validate(){}
	
	public static void clear(){
		invoked.clear();
		invocations.clear();
		actionIDs.clear();
	}
	
	public static synchronized void actionCreated(String id){
		actionIDs.add(id);
	}
	
	public static List<String> actionIDs(){
		return actionIDs;
	}
		
	public static synchronized void invoked(String id){
		Integer i=invocations.get(id);
		if(i==null)i=Integer.valueOf(0);
		i++;
		invocations.put(id, i);
		invoked.add(id);
	}
	
	/**
	 * check that the activity with the given ID was invoked 
	 */
	public static boolean wasInvoked(String id){
		return invocations.keySet().contains(id);
	}
	
	/**
	 * check that the activity with the given ID was invoked 
	 */
	public static Integer getInvocations(String id){
		return invocations.get(id);
	}
	

	/**
	 * check that the activity "id1" was invoked before "id2" 
	 * @param id1 - the id of the "first" activity
	 * @param id2 - the id of the "second" activity
	 */
	public static boolean before(String id1, String id2){
		int i1=invoked.indexOf(id1);
		int i2=invoked.indexOf(id2);
		return i1<i2;
	}
}
