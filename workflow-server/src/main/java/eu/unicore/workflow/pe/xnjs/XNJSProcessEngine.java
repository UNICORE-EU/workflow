package eu.unicore.workflow.pe.xnjs;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.ProcessEngine;
import eu.unicore.workflow.pe.ProcessState;
import eu.unicore.workflow.pe.ProcessState.State;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.PEWorkflow;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;

/**
 * XNJS based process engine implementation
 *
 * @author schuller
 */
public class XNJSProcessEngine implements ProcessEngine {

	private final XNJS xnjs;

	public XNJSProcessEngine(XNJS xnjs){
		this.xnjs=xnjs;
	}

	public void process(PEWorkflow workflow, SecurityTokens securityContext) throws Exception{
		process(workflow,securityContext,null,null);
	}

	/**
	 * start processing a workflow. This will add the required entry to the persisted data
	 */
	public void process(PEWorkflow workflow, SecurityTokens securityTokens, String storageURL, Calendar terminationTime) throws Exception {
		workflow.init();
		Action action=new Action();
		action.setUUID(workflow.getWorkflowID());
		action.setAjd(workflow);
		action.setType(ActivityGroup.ACTION_TYPE);
		ProcessVariables vars=new ProcessVariables();
		action.getProcessingContext().put(ProcessVariables.class, vars);
		action.getProcessingContext().put(Constants.PV_KEY_ITERATION, "");
		WorkflowContainer attr=new WorkflowContainer();
		attr.build(workflow);
		if(securityTokens!=null && securityTokens.getUserName()!=null) {
			attr.setUserDN(securityTokens.getUserName());
		}
		else {
			attr.setUserDN(Client.ANONYMOUS_CLIENT_DN);
		}
		attr.setStorageURL(storageURL);
		attr.setLifetime(terminationTime);
		PEConfig.getInstance().getPersistence().write(attr);
		xnjs.get(Manager.class).add(action, null);
	}

	public ProcessState getProcessState(String workflowID) 
			throws Exception {
		ProcessState state=new ProcessState();

		Action action = xnjs.get(InternalManager.class).getAction(workflowID);
		if(action!=null){

			int status=action.getStatus();
			if(ActionStatus.DONE==status){
				if(ActionResult.USER_ABORTED==action.getResult().getStatusCode()){
					state.setState(ProcessState.State.ABORTED);
				}
				else if(!action.getResult().isSuccessful()){
					state.setState(ProcessState.State.FAILED);
				}
				else{
					state.setState(ProcessState.State.SUCCESSFUL);
				}
			}
			else {
				state.setState(ProcessState.State.RUNNING);
			}

			//check for user input needed, i.e. hold state in some sub-flow
			try{
				WorkflowContainer wfc=PEConfig.getInstance().getPersistence().read(workflowID);
				if(wfc!=null){
					if(isHeld(wfc)){
						state.setState(ProcessState.State.HELD);
					}
				}
			}
			catch(Exception ex){}
			state.setVariables(action.getProcessingContext().get(ProcessVariables.class).copy());
		}
		return state;
	}

	public void abort(String workflowID) 
			throws Exception {
		ProcessState ps = getProcessState(workflowID);
		xnjs.get(Manager.class).abort(workflowID, null);
		if(State.HELD.equals(ps.getState())){
			resume(workflowID, new HashMap<String,String>()); 
		}
	}

	public void destroy(String workflowID, SecurityTokens securityContext) 
			throws Exception {
		xnjs.get(Manager.class).destroy(workflowID, null);
	}

	public void resume(String workflowID, Map<String,String>params) throws Exception {
		try(WorkflowContainer wfc = PEConfig.getInstance().getPersistence().getForUpdate(workflowID)){
			doResume(wfc,params);
		}
	}

	public XNJS getXNJS() {
		return xnjs;
	}

	private void doResume(SubflowContainer sfc,Map<String,String>params){
		if(sfc.isHeld()){
			sfc.resume(params);
		}
		for(SubflowContainer sub : sfc.getSubFlowAttributes()){
			doResume(sub,params);
		}
	}

	private boolean isHeld(SubflowContainer sfc){
		if(sfc.isHeld()){
			return true;
		}
		for(SubflowContainer sub : sfc.getSubFlowAttributes()){
			return isHeld(sub);
		}
		return false;
	}
}
