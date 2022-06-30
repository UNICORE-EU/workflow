package eu.unicore.workflow.json;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chemomentum.dsws.ConversionResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.util.Log;
import eu.unicore.workflow.EvaluationFunctions;
import eu.unicore.workflow.pe.Evaluator;
import eu.unicore.workflow.pe.iterators.ChunkedFileIterator;
import eu.unicore.workflow.pe.iterators.CounterIteration;
import eu.unicore.workflow.pe.iterators.FileSetIterator;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.workflow.pe.iterators.Iteration;
import eu.unicore.workflow.pe.iterators.ValueSetIterator;
import eu.unicore.workflow.pe.iterators.VariableSetIterator;
import eu.unicore.workflow.pe.iterators.VariableSetIterator.VariableSet;
import eu.unicore.workflow.pe.model.Activity.MergeType;
import eu.unicore.workflow.pe.model.Activity.SplitType;
import eu.unicore.workflow.pe.model.ActivityContainer;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.ForGroup;
import eu.unicore.workflow.pe.model.HoldActivity;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.JSONExecutionActivity;
import eu.unicore.workflow.pe.model.ModifyVariableActivity;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.model.RepeatGroup;
import eu.unicore.workflow.pe.model.RoutingActivity;
import eu.unicore.workflow.pe.model.ScriptCondition;
import eu.unicore.workflow.pe.model.WhileGroup;
import eu.unicore.workflow.pe.util.TestActivity;
import eu.unicore.workflow.pe.xnjs.Constants;

public class Converter {

	//understood activity option names
	public static final Set<String> CONVERTER_OPTIONS = 
			Collections.unmodifiableSet(new HashSet<>());

	private final boolean unitTesting;

	public Converter(){
		this(false);
	}

	public Converter(boolean unitTesting){
		this.unitTesting = unitTesting;
	}

	/**
	 * generate internal workflow representation from the supplied top-level workflow instance
	 * 
	 * @param wfUUID - the workflow instance ID
	 * @param wf - the simple workflow
	 * t
	 * @return the {@link ConversionResult} for the workflow instance
	 */
	public ConversionResult convert(String wfUUID, JSONObject wf) throws Exception {
		List<String>outputFiles = new ArrayList<>();
		ConversionResult result = new ConversionResult();
		result.setWorkflowID(wfUUID);
		wf.put("id", wfUUID);
		PEWorkflow internalWorkflow = (PEWorkflow)convertSubFlow(wf,outputFiles, null, result);
		result.setConvertedWorkflow(internalWorkflow);
		result.setOutputFiles(outputFiles);
		return result;
	}

	/**
	 * build the ActivityGroup corresponding to a sub-workflow
	 */
	protected ActivityGroup convertSubFlow(JSONObject wf,
			List<String>outputFiles, String parentLoopID, ConversionResult result) throws JSONException {

		WorkflowInfo workflowInfo = WorkflowInfo.buildSubflow(wf);
		String subID = wf.optString("id");

		ActivityGroup converted;

		if(subID.equals(result.getWorkflowID())){
			converted=new PEWorkflow(result.getWorkflowID());
			
		}
		else{
			converted=new ActivityGroup(subID,result.getWorkflowID());
			boolean coBroker = Boolean.parseBoolean(getOption(wf, "COBROKER", "false"));
			converted.setCoBrokerActivities(coBroker);
		}
		
		String notification = wf.optString("notification", null);
		if(notification==null)notification = wf.optString("Notification", null);
		converted.setNotificationURL(notification);

		if(workflowInfo.isLoop()){
			parentLoopID=workflowInfo.getIteratorName();
			converted.setLoopIteratorName(parentLoopID);
		}

		addVariableDeclarations(workflowInfo, converted, result);

		//this list contains all the activities in this subgroup 
		List<eu.unicore.workflow.pe.model.Activity>activities=new ArrayList<>();
		workflowInfo.getSubWorkflows();
		for(JSONObject swf: workflowInfo.getSubWorkflows()) {
			String id = swf.getString("id");
			eu.unicore.workflow.pe.model.Activity sub=null;
			String type = swf.optString("type", "GROUP");
			try {
				if("FOR_EACH".equals(type)){
					sub=processForEach(swf, outputFiles, parentLoopID, result);
				}
				else if("REPEAT_UNTIL".equals(type)){
					sub=processRepeatUntil(swf, outputFiles, parentLoopID, result);
				}
				else if("WHILE".equals(type)){
					sub=processWhile(swf, outputFiles, parentLoopID, result);
				}
				else{
					sub=convertSubFlow(swf,outputFiles, parentLoopID, result);
				}
				if(sub!=null)activities.add(sub);
			}catch(Exception ex){
				String msg="Subgroup '"+id+"': got an exception during conversion";
				result.addError(Log.createFaultMessage(msg, ex));
			}
		}
		//block start?
		if(workflowInfo.getStartActivities().size()==0){
			//may have a subflow as start activity
			List<JSONObject>start = workflowInfo.getStartSubflows();
			if(start.size()!=1){
				result.addError("(Sub-)workflow '"+workflowInfo.getId()+"': does not have a well-defined beginning!");
			}
			else{
				//add a start activity and transition to original start
				String originalStart=start.get(0).getString("id");
				String newStart = "START_"+originalStart+"-"+newUID();
				JSONObject startActivity = new JSONObject();
				startActivity.put("type", "START");
				startActivity.put("id", newStart);
				workflowInfo.addActivity(startActivity);
				//add transition from new start to subflow
				workflowInfo.addTransition(newStart, originalStart);
			}
		}

		//block end?
		if(workflowInfo.getEndActivities().size()==0){
			//may have a subflow as start activity
			List<JSONObject>end = workflowInfo.getEndSubflows();
			if(end.size()!=1){
				//no error, there are legal workflows that have no well-defined end
			}
			else{
				//add an end activity and transition to original end
				String originalEnd=end.get(0).getString("id");
				String newEnd = "END_"+originalEnd+"-"+newUID();
				JSONObject endActivity = new JSONObject();
				endActivity.put("type", "MERGE");
				endActivity.put("id", newEnd);
				workflowInfo.addActivity(endActivity);
				//add transition to the new end activity
				workflowInfo.addTransition(originalEnd,newEnd);
			}
		}

		for(JSONObject a: workflowInfo.getActivities()){
			processActivity(a, workflowInfo, activities, result, outputFiles);
		}

		converted.setActivities(activities);

		//add transitions
		addTransitions(workflowInfo, converted, result);

		return converted;
	}

	/**
	 * build a "for-each" group 
	 */
	protected eu.unicore.workflow.pe.model.Activity processForEach(JSONObject wf,
			List<String>outputFiles, String parentLoopID, ConversionResult result)throws Exception{
		String id = wf.getString("id");
		String iteratorName = wf.optString("iterator_name", "IT");
		
		Iterate iterate = buildIterator(wf, result.getWorkflowID(), result);
		JSONObject body = wf.getJSONObject("body");
		if(body.optString("id", null)==null) {
			body.put("id",id+"__body__");
		}
		ActivityGroup bodyGroup=convertSubFlow(body, outputFiles, parentLoopID, result);

		bodyGroup.setIterate(iterate);
		bodyGroup.setLoopIteratorName(iteratorName);
		ForGroup res = new ForGroup(id, result.getWorkflowID(), bodyGroup);

		String maxConcurrent = getOption(wf, ForGroup.PROPERTY_MAX_CONCURRENT_ACTIVITIES);
		if(maxConcurrent!=null){
			try{
				Integer maxConcurrentValue=Integer.parseInt(maxConcurrent);
				res.setMaxConcurrentActivities(maxConcurrentValue);
			}catch(NumberFormatException nfe){
				result.addError("For-each loop '"+id+"': Parameter '"+
						ForGroup.PROPERTY_MAX_CONCURRENT_ACTIVITIES+"' does not evaluate to an integer");
				return null;
			}
		}

		return res;
	}

	/**
	 * build the iterator for a "for-each" group
	 */
	protected static Iterate buildIterator(JSONObject wf, String wfID, ConversionResult result)throws Exception{
		String id = wf.getString("id");
		Iteration iterate=null;
		JSONArray values = wf.optJSONArray("values");
		JSONArray fileSetDef = wf.optJSONArray("filesets");
		JSONArray variableSets = wf.optJSONArray("variables");

		if(values!=null && values.length()>0){
			iterate=new ValueSetIterator(JSONUtil.toArray(values));
		}
		else if(fileSetDef!=null && fileSetDef.length()>0){
			List<FileSet>fileSets=new ArrayList<FileSet>();
			boolean chunked=false;
			int chunkSize=0;
			String formatString=null;
			String expression = null;
			for(int i=0; i<fileSetDef.length(); i++){
				JSONObject fileSet = fileSetDef.getJSONObject(i);
				boolean recurse = fileSet.optBoolean("recurse", false);
				boolean indirection = fileSet.optBoolean("indirection", false);
				String base = fileSet.getString("base");
				String[] excl = JSONUtil.toArray(fileSet.optJSONArray("exclude"));
				String[] incl = JSONUtil.toArray(fileSet.optJSONArray("include"));
				fileSets.add(new FileSet(base,incl,excl,recurse,indirection));
			}
			JSONObject chunk = wf.optJSONObject("chunking");
			ChunkedFileIterator.Type chunkType = ChunkedFileIterator.Type.NUMBER;
			if(chunk!=null){
				chunked=true;
				chunkSize = chunk.optInt("chunksize", -1);
				chunkType = ChunkedFileIterator.Type.valueOf(chunk.optString("type", "NUMBER"));
				expression = chunk.optString("expression", null);
				
				if(chunkSize<0){
					result.addError("For-each loop '"+id+"': chunk size  must be an integer value larger than 0. Got: "+chunkSize);
					chunked=false;
				}
				formatString = chunk.optString("filename_format", null);
				//test format now
				if(formatString!=null){
					try{
						ChunkedFileIterator.testFormat(formatString);
					}catch(IllegalArgumentException e){
						result.addError(Log.createFaultMessage("For-each loop '"+id+"': Error in format string '"+formatString+"'", e));
					}
				}
			}
			FileSetIterator fileSetIterator=new FileSetIterator(wfID, fileSets.toArray(new FileSetIterator.FileSet[fileSets.size()]));
			//add chunking
			if(chunked){
				if(expression!=null) {
					iterate=new ChunkedFileIterator(fileSetIterator, expression, chunkType);
				}
				else {
					iterate=new ChunkedFileIterator(fileSetIterator, chunkSize, chunkType);
				}
				if(formatString!=null)((ChunkedFileIterator)iterate).setFormatString(formatString);
			}
			else{
				iterate=fileSetIterator;
			}
		}
		else if(variableSets!=null && variableSets.length()>0){
			List<VariableSet>varSets=new ArrayList<VariableSet>();
			for(int i=0; i<variableSets.length(); i++){
				JSONObject var = variableSets.getJSONObject(i);
				String vName = var.getString("variable_name");
				String vType = var.optString("type", "INTEGER");
				String expr = var.getString("expression");

				//condition can contain "eval(....)"
				String condScript = var.getString("end_condition").trim();
				if(condScript.startsWith("eval(") && condScript.endsWith(")")){
					condScript=condScript.substring(condScript.indexOf('(')+1, condScript.lastIndexOf(')'));
				}
				if(!condScript.endsWith(";"))condScript+=";";
				String start = var.getString("start_value");
				varSets.add(new VariableSet(vName,start,condScript,expr,vType));
			}
			iterate=new VariableSetIterator(wfID,varSets.toArray(new VariableSet[varSets.size()]));
		}
		if(iterate!=null){
			iterate.setIteratorName(wf.optString("iterator_name", "IT"));
		}
		else{
			result.addError("For-each loop '"+id+"' does not define an iterator.");
		}
		return iterate;
	}

	/**
	 * build a "repeat-until" group
	 */
	protected eu.unicore.workflow.pe.model.Activity processRepeatUntil(JSONObject wf,
			List<String>outputFiles, String parentLoopID, ConversionResult result)throws Exception{

		String id = wf.getString("id");
		eu.unicore.workflow.pe.model.Condition condition=buildCondition(wf, result.getWorkflowID());
		JSONObject body = wf.getJSONObject("body");
		if(body.optString("id", null)==null) {
			body.put("id", id+"__body__");
		}
		ActivityGroup bodyGroup = convertSubFlow(body, outputFiles, parentLoopID, result);	
		
		RepeatGroup res = new RepeatGroup(id,result.getWorkflowID(),bodyGroup, condition);
		WorkflowInfo workflowInfo = WorkflowInfo.build(wf);
		addVariableDeclarations(workflowInfo, res, result);
		
		Iterate iterate=new Iteration();
		String iteratorName = wf.optString("iterator_name", null);
		if(iteratorName == null) {
			iterate = new CounterIteration();
		}
		else {
			((Iteration)iterate).setIteratorName(iteratorName);
		}
		bodyGroup.setIterate(iterate);
		bodyGroup.setLoopIteratorName(iteratorName);
		return res;
	}

	/**
	 * build a "while" group
	 */
	protected eu.unicore.workflow.pe.model.Activity processWhile(JSONObject wf,
			List<String>outputFiles, String parentLoopID, ConversionResult result)throws Exception {
		String id = wf.getString("id");
		eu.unicore.workflow.pe.model.Condition condition=buildCondition(wf, result.getWorkflowID());
		JSONObject body = wf.getJSONObject("body");
		body.put("is_loop_body", "true");
		if(body.optString("id", null)==null) {
			body.put("id", id+"__body__");
		}
		ActivityGroup bodyGroup = convertSubFlow(body, outputFiles, parentLoopID, result);	
	
		WhileGroup res = new WhileGroup(id,result.getWorkflowID(),bodyGroup, condition);
		WorkflowInfo workflowInfo=WorkflowInfo.build(wf);
		addVariableDeclarations(workflowInfo, res, result);
		
		Iterate iterate=new Iteration();
		String iteratorName = wf.optString("iterator_name", null);
		if(iteratorName == null) {
			iterate = new CounterIteration();
		}
		else {
			((Iteration)iterate).setIteratorName(iteratorName);
		}
		bodyGroup.setIterate(iterate);
		bodyGroup.setLoopIteratorName(iteratorName);
		
		return res;
	}


	/**
	 * build the condition for a "while" group
	 */
	protected static eu.unicore.workflow.pe.model.Condition buildCondition(JSONObject wf, String wfID) throws Exception {
		String expr = wf.getString("condition");
		String convertedExpr = convertExpression(wfID, expr);
		String cID = wf.getString("id")+"_condition";
		ScriptCondition result = new ScriptCondition(cID, wfID, convertedExpr);
		return result;
	}

	/**
	 * build an activity
	 */
	protected void processActivity(JSONObject a, 
			WorkflowInfo workflowInfo,
			List<eu.unicore.workflow.pe.model.Activity>activities,
			ConversionResult result, List<String> outputFiles) throws JSONException {

		boolean err = checkActivityErrors(a,workflowInfo,result);

		if(err){
			return;
		}

		String id = a.getString("id");

		String activityType = a.optString("type", null);

		if(activityType==null && a.optJSONObject("job")!=null) {
			activityType = "JOB";
		}

		if("JOB".equals(activityType)){
			if(unitTesting) {
				activities.add(new TestActivity(id, result.getWorkflowID()));
				return;
			}
			
			JSONObject job = a.optJSONObject("job");
			if(job!=null){
				addWork(a,outputFiles,activities,workflowInfo,result);
			}
			else {
				result.addError("Job activity '"+id+"': no job definition found.");
				err = true;
			}
		}
		else if ("MODIFY_VARIABLE".equals(activityType)){
			String name = getOption(a, "variable_name");
			String expr = getOption(a, "expression");

			if(name==null || expr==null){
				result.addError("Variable modification '"+id+"': need 'variable_name' and 'expression' options.");
				err = true;
			}
			else{
				addVariableModification(a,outputFiles,activities,workflowInfo,result);
			}
		}
		else if ("START".equals(activityType)){
			RoutingActivity r = new RoutingActivity(id,result.getWorkflowID());
			activities.add(r);
		}

		else if ("SPLIT".equals(activityType)){
			RoutingActivity r = new RoutingActivity(id,result.getWorkflowID());
			r.setSplitType(SplitType.FOLLOW_ALL_MATCHING);
			activities.add(r);
		}

		else if ("BRANCH".equals(activityType)){
			RoutingActivity r = new RoutingActivity(id,result.getWorkflowID());
			r.setSplitType(SplitType.FOLLOW_FIRST_MATCHING);
			activities.add(r);
		}

		else if ("MERGE".equals(activityType)){
			RoutingActivity r = new RoutingActivity(id,result.getWorkflowID());
			r.setMergeType(MergeType.MERGE);
			activities.add(r);
		}

		else if ("SYNCHRONIZE".equals(activityType)){
			RoutingActivity r=new RoutingActivity(id,result.getWorkflowID());
			r.setMergeType(MergeType.SYNCHRONIZE);
			activities.add(r);
		}

		else if ("HOLD".equals(activityType)){
			HoldActivity r=new HoldActivity(id,result.getWorkflowID());
			r.setMergeType(MergeType.SYNCHRONIZE);
			activities.add(r);
		}

		else if ("PAUSE".equals(activityType)){
			HoldActivity r=new HoldActivity(id,result.getWorkflowID());
			r.setMergeType(MergeType.SYNCHRONIZE);
			String sleepTime = getOption(a, "sleepTime");
			if(sleepTime!=null){
				try{
					r.setSleepTime(Long.valueOf(sleepTime));
				}
				catch(NumberFormatException nfe){
					result.addError("Activity '"+id+"': parameter 'sleepTime' must be of type 'long'");
					err = true;
				}
			}
			activities.add(r);
		}

		else if (!err){
			result.addError("Activity '"+id+"': unknown activity type <"+activityType+">");
			err = true;
		}

		if(err) {
			// add a dummy activity with the correct ID to avoid follow-on errors 
			// that "distract" the user from the real error
			activities.add(new RoutingActivity(id, result.getWorkflowID()));
		}
	}


	/**
	 * add transitions within a sub.flow 
	 */
	protected static void addTransitions(WorkflowInfo workflowInfo, ActivityGroup subflow, ConversionResult result) throws JSONException {
		List<eu.unicore.workflow.pe.model.Transition>resultTransitions = new ArrayList<>();
		List<JSONObject>transitions=workflowInfo.getTransitions();
		for(JSONObject t: transitions){

			String fromID = t.getString("from");
			String toID = t.getString("to");
			
			eu.unicore.workflow.pe.model.Activity from = subflow.getActivity(fromID);
			eu.unicore.workflow.pe.model.Activity to = subflow.getActivity(toID);
			String transitionID = t.optString("id", newUID());

			if(from==null){
				result.addError("Transition "+fromID+"->"+toID+" : references non-existent 'from' entity <"+fromID+">");
				continue;
			}
			if(to==null){
				result.addError("Transition "+fromID+"->"+toID+": references non-existent 'to' entity <"+toID+">");
				continue;
			}

			eu.unicore.workflow.pe.model.Condition convertedCondition=null;

			try{
				String condition = t.optString("condition", null);
				if(condition!=null){
					String convertedExpr = convertExpression(result.getWorkflowID(), condition);
					convertedCondition = new eu.unicore.workflow.pe.model.ScriptCondition("condition-"
							+transitionID, result.getWorkflowID(), convertedExpr);
				}
			}catch(IllegalArgumentException iae){
				String msg=Log.createFaultMessage("Transition '"+transitionID+"': error processing.", iae);
				result.addError(msg);
				continue;
			}

			eu.unicore.workflow.pe.model.Transition converted=new eu.unicore.workflow.pe.model.Transition(transitionID,result.getWorkflowID(),from.getID(),to.getID(),convertedCondition);
			resultTransitions.add(converted);
		}
		subflow.setTransitions(resultTransitions.toArray(new eu.unicore.workflow.pe.model.Transition[]{}));
	}

	/**
	 * build a {@link JSONExecutionActivity} from a workflow activity
	 */
	protected static void addWork(JSONObject a, List<String>outputFiles, List<eu.unicore.workflow.pe.model.Activity>activities, WorkflowInfo workflowInfo,ConversionResult result){
		String id = null;
		try{
			id = a.getString("id");

			JSONExecutionActivity work=new JSONExecutionActivity(id, result.getWorkflowID());
			work.setJobDefinition(a.getJSONObject("job"));

			JSONObject options = a.optJSONObject("options");
			if(options!=null) {
				@SuppressWarnings("unchecked")
				Iterator<String> keys = (Iterator<String>)options.keys();
				while(keys.hasNext()){
					String key = keys.next();;
					if(!JSONExecutionActivity.UNDERSTOOD_OPTIONS.contains(key)
							&&!CONVERTER_OPTIONS.contains(key))
					{
						result.addError("Activity '"+id+"': uses non-supported option '"+key+"'");
					}
					String value = options.getString(key);
					work.setOption(key, value);
				}
			}
			activities.add(work);
		}
		catch(Exception e){
			result.addError(Log.createFaultMessage("Activity '"+id+"': error converting.",e));
		}
	}

	/**
	 * process variable declarations
	 */
	protected static void addVariableDeclarations(WorkflowInfo workflowInfo, ActivityContainer target, ConversionResult result){
		List<DeclareVariableActivity>activities = new ArrayList<>();
		for(JSONObject dv:workflowInfo.getDeclarations()){
			boolean OK = true;

			String id = dv.optString("id", null);
			String name = dv.optString("name", null);
			String type = dv.optString("type", null);
			String initialValue = dv.optString("initial_value", "");

			if(type==null){
				result.addError("Variable declaration '"+id+"': need a type.");
				OK=false;
			}
			if(name==null){
				result.addError("Variable declaration '"+id+"': need a name.");
				OK=false;
			}

			if(id==null){
				if(OK) {
					id = "declare-"+name+"-"+type;
				}
			}

			if(OK){
				DeclareVariableActivity decl=new DeclareVariableActivity(id,result.getWorkflowID(),name,type,initialValue);
				activities.add(decl);
				result.getDeclaredVariables().put(name,type);
			}
		}
		target.setDeclarations(activities);
	}

	/**
	 * add a variable modification activity 
	 */
	protected static void addVariableModification(JSONObject a, 
			List<String>outputFiles, 
			List<eu.unicore.workflow.pe.model.Activity>activities, 
			WorkflowInfo workflowInfo,
			ConversionResult result)
	{
		String id = null;
		try{
			id = a.getString("id");
			String wfid = result.getWorkflowID();
			String varName = getOption(a, "variable_name");
			String expression = getOption(a, "expression");
			String script = convertExpression(wfid, expression);
			ModifyVariableActivity modVar = new ModifyVariableActivity(id,wfid,varName,script);
			activities.add(modVar);
		}
		catch(Exception e){
			result.addError(Log.createFaultMessage("Error converting activity <"+id+">",e));
		}
	}

	static char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	static Random random = new Random();

	private static synchronized String newUID() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			sb.append(chars[random.nextInt(chars.length)]);
		}
		return sb.toString();
	}

	private static String getOption(JSONObject a, String[] optionNames){
		String res = null;
		for(String o: optionNames) {
			res = getOption(a, o, null);
			if(res!=null)return res;
		}
		return res;
	}
	
	private static String getOption(JSONObject a, String optionName){
		return getOption(a, optionName, null);
	}

	private static String getOption(JSONObject a, String optionName, String defaultValue){
		JSONObject opts = a.optJSONObject("options");
		if(opts!=null) {
			return opts.optString(optionName, defaultValue);
		}
		else {
			return a.optString(optionName, defaultValue);
		}
	}

	/**
	 * check for errors in the activity definition
	 * @return <code>true</code> if there are errors in the activity
	 */
	private static boolean checkActivityErrors(JSONObject a, WorkflowInfo workflowInfo, ConversionResult result){
		int initialErrors=result.getConversionErrors().size();
		String activityID = a.optString("id", null);
		if(activityID==null){
			result.addError("Activity needs ID: "+a.toString());
		}

		String activityType = a.optString("type", null);
		if(activityType==null && a.optJSONObject("job")==null){
			result.addError("Activity '"+activityID+"' needs a 'type' attribute");
		}

		return result.getConversionErrors().size()>initialErrors;
	}


	// available evaluation functions
	private static final List<Pattern> methodPatterns=new ArrayList<>();

	static {
		for(Method m: EvaluationFunctions.class.getMethods()){
			methodPatterns.add(Pattern.compile(m.getName()+"\\([^\\)]*\\)"));
		}
	}

	/**
	 * replace all occurrences of evaluation functions by full, working
	 * Groovy code for running the function
	 * 
	 * @param workflowID
	 * @param expression
	 */
	public static String convertExpression(String workflowID,String expression){
		String res=expression;
		for(Pattern p: methodPatterns){
			Matcher m=p.matcher(res);
			while(m.find()){
				String matched = m.group();
				String replacement = "(new "+Evaluator.class.getName()+"(\""+workflowID+"\", "+
						Constants.VAR_KEY_CURRENT_TOTAL_ITERATION+")."+matched+")";
				res = res.replace(matched, replacement);
			}
		}
		return res;

	}
}
