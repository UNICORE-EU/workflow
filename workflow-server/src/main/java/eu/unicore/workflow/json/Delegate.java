package eu.unicore.workflow.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chemomentum.dsws.ConversionResult;
import org.chemomentum.dsws.DSLDelegate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.security.SecurityTokens;
import eu.unicore.util.Log;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.IterationEqualsFilter;
import eu.unicore.workflow.pe.util.ParentIterationFilter;

/**
 * Accept new workflows and create status reports for the "simple workflow" dialect 
 *
 * @author schuller
 */
public class Delegate implements DSLDelegate {

	public static final String DIALECT="https://www.unicore.eu/workflow/json";

	public ConversionResult addNewWorkflow(String uniqueID, Object wf, SecurityTokens tokens) {
		try{
			return new Converter().convert(uniqueID, (JSONObject)wf);
		}catch(Exception e){
			throw new IllegalArgumentException(Log.createFaultMessage("Can't process workflow", e), e);
		}
	}


	public static String convertStatus(eu.unicore.workflow.pe.model.ActivityStatus val){
		boolean stillRunning=false;
		boolean success=true;
		switch(val){
		case CREATED:
			return "NOT_STARTED";
		case RUNNING:
			stillRunning=true;
			break;
		case FAILED:
			success=false;
			break;
		default:
		}
		if(stillRunning)return "RUNNING";
		else if(success)return "SUCCESSFUL";
		else return "FAILED";

	}

	public String getDialect() {
		return DIALECT;
	}


	public Map<String,Object> getStatus(String uniqueID) throws Exception {
		Map<String,Object> st = new HashMap<>();
		WorkflowContainer wfc = PEConfig.getInstance().getPersistence().read(uniqueID);
		if(wfc==null)return st;

		//top level activities
		Collection<String> activities = wfc.getActivities();
		Map<String,Object> activityStati = new HashMap<>();
		for(String id: activities){
			if(wfc.isSubFlow(id))continue;
			activityStati.put(id, getActivityStatusJ(wfc,uniqueID, id, null));
		}
		st.put("activities", activityStati);

		List<SubflowContainer> subworkflows=wfc.getSubFlowAttributes();
		Map<String, Object> subworkflowStati = new HashMap<>();
		for(SubflowContainer subFlow: subworkflows){
			subworkflowStati.put(subFlow.getId(), getSubworkflowStatusJ(uniqueID,subFlow,wfc,null));
		}
		st.put("subworkflows", subworkflowStati);
		return st;
	}

	protected List<Object> getActivityStatusJ(SubflowContainer subFlowInfo, String workflowID, String activityID, String parentIterationValue){
		List<Object> as = new ArrayList<>();
		List<PEStatus> activityStati=subFlowInfo.getActivityStatus(activityID,new IterationEqualsFilter(parentIterationValue));
		if(activityStati==null){
			return as;
		}
		if(activityStati.size()>0){
			for(PEStatus s: activityStati){
				Map<String,Object> ase = new HashMap<>();
				String iteration = s.getIteration();
				ase.put("iteration", iteration);

				if(s.getActivityStatus().equals(eu.unicore.workflow.pe.model.ActivityStatus.FAILED)){
					String errorCode = s.getErrorCode();
					String error = s.getErrorDescription();
					ase.put("errorMessage", errorCode+": "+error);
				}
				ase.put("status", convertStatus(s.getActivityStatus()));
				as.add(ase);
			}
		}
		else{
			Map<String,Object> ase = new HashMap<>();
			if(parentIterationValue!=null)ase.put("iteration", parentIterationValue);
			ase.put("status", "NOT_STARTED");
			as.add(ase);
		}

		return as;
	}

	protected JSONArray getSubworkflowStatusJ(String workflowID, SubflowContainer subFlowAttributes, 
			SubflowContainer parentAttributes, String parentIterationValue) throws JSONException {
		JSONArray sws = new JSONArray();
		List<PEStatus> iterations=parentAttributes.getActivityStatus(subFlowAttributes.getId(),new ParentIterationFilter(parentIterationValue));
		for(PEStatus p: iterations)
		{	
			JSONObject swr = new JSONObject();

			String thisIteratorValue;

			thisIteratorValue=p.getIteration();

			String iteration;
			if(thisIteratorValue!=null){
				iteration=thisIteratorValue;
				swr.put("iteration", iteration);
			}
			if(subFlowAttributes.isSplit()){
				swr.put("iteration", "1");
			}

			//activities
			Collection<String> activities = subFlowAttributes.getActivities();

			Map<String, List<Object>> activityStati = new HashMap<>();
			for(String act: activities){
				if(subFlowAttributes.isSubFlow(act))continue;
				activityStati.put(act, getActivityStatusJ(subFlowAttributes, workflowID, act,thisIteratorValue));
			}
			swr.put("activities", activityStati);

			//subworkflows
			List<SubflowContainer> subworkflows=subFlowAttributes.getSubFlowAttributes();
			Map<String,JSONArray>subWorkflowStati = new HashMap<>();

			for(SubflowContainer sub: subworkflows){
				String subIterationParent=thisIteratorValue!=null?thisIteratorValue:parentIterationValue;
				subWorkflowStati.put(sub.getId(), 
						getSubworkflowStatusJ(workflowID, sub, subFlowAttributes, subIterationParent));
			}
			swr.put("subworkflows", subWorkflowStati);

			//write toplevel status
			if(thisIteratorValue!=null){
				try{
					PEStatus s = parentAttributes.getActivityStatus(subFlowAttributes.getId(), thisIteratorValue);
					String swfState = convertStatus(s.getActivityStatus());
					swr.put("status", swfState);
					if("FAILED".equals(swfState)){
						//write error info
						swr.put("errorMessage", s.getErrorCode()+" "+s.getErrorDescription());
					}
				}catch(IllegalArgumentException iae){
					swr.put("status", "ERROR");
					swr.put("errorMessage", Log.createFaultMessage("Can't set overall sub-workflow status", iae));
				}
			}
			sws.put(swr);
		}

		return sws;
	}

}
