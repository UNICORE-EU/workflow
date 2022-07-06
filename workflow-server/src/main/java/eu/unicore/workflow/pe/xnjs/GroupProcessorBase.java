package eu.unicore.workflow.pe.xnjs;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import eu.unicore.workflow.WorkflowProperties;
import eu.unicore.workflow.pe.PEConfig;
import eu.unicore.workflow.pe.model.Activity;
import eu.unicore.workflow.pe.model.ActivityContainer;
import eu.unicore.workflow.pe.model.ActivityGroup;
import eu.unicore.workflow.pe.model.ActivityStatus;
import eu.unicore.workflow.pe.model.DeclareVariableActivity;
import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.model.util.VariableUtil;
import eu.unicore.workflow.pe.persistence.PEStatus;
import eu.unicore.workflow.pe.persistence.SubflowContainer;
import eu.unicore.workflow.pe.persistence.WorkflowContainer;
import eu.unicore.workflow.pe.util.WorkAssignmentUtils;

/**
 * base processor for groups and control constructs (for, repeat, ...) 
 * 
 * @author schuller
 */
public class GroupProcessorBase extends ProcessorBase {

	private static final String SUBACTIONS_KEY = "SUBACTIONS";
	
	public GroupProcessorBase(XNJS configuration) {
		super(configuration);
	}

	@SuppressWarnings("unchecked")
	protected List<String> getOrCreateSubTasks(){
		List<String> res = (List<String>) action.getProcessingContext().get(SUBACTIONS_KEY);
		if(res==null){
			res = new ArrayList<String>();
			action.getProcessingContext().put(SUBACTIONS_KEY,res);
			action.setDirty();
		}
		return res;
	}

	protected String submit(Activity a, SubflowContainer attr)throws ExecutionException{
		ProcessVariables vars=new ProcessVariables();
		vars.putAll(action.getProcessingContext().get(ProcessVariables.class).copy());
		incrementCounterAndCheckMaxActivities();
		//compute next iteration value
		Iterate iterate=a.getIterate();
		String iteration;
		String base=getCurrentIteration();
		iterate.setBase(base);
		iterate.next(vars);
		iterate.fillContext(vars);
		iteration=iterate.getCurrentValue();
		//setup XNJS action
		String uuid=submitToXNJS(a, vars, iteration);
		a.setStatus(ActivityStatus.RUNNING);
		PEStatus activityStatus=new PEStatus();
		activityStatus.setActivityStatus(ActivityStatus.RUNNING);
		activityStatus.setIteration(iterate.getCurrentValue());
		attr.getActivityStatus(a.getID()).add(activityStatus);
		action.setDirty();
		return uuid;
	}

	protected String submit(ActivityGroup group, SubflowContainer attr)throws ExecutionException{
		ProcessVariables vars=new ProcessVariables();
		vars.putAll(action.getProcessingContext().get(ProcessVariables.class).copy());
		incrementCounterAndCheckMaxActivities();
		Iterate iterate=group.getIterate();
		String base=getCurrentIteration();
		iterate.setBase(base);
		String iteration;
		iterate.next(vars);
		iterate.fillContext(vars);
		iteration=iterate.getCurrentValue();
		//setup XNJS action
		String uuid=submitToXNJS(group, vars,iteration);
		PEStatus activityStatus=new PEStatus();
		activityStatus.setActivityStatus(ActivityStatus.RUNNING);
		activityStatus.setIteration(iteration);
		attr.getActivityStatus(group.getID()).add(activityStatus);
		logger.info("Submitting activity group <"+group.getID()+"> status "+activityStatus);
		group.setStatus(ActivityStatus.RUNNING);
		action.setDirty();
		return uuid;
	}


	protected String submitToXNJS(Activity a, ProcessVariables vars, String iteration)throws ExecutionException{
		Action subAction=new Action();
		subAction.setType(a.getType());
		subAction.setAjd(a.clone());
		subAction.setClient(action.getClient());
		String uid=WorkAssignmentUtils.buildActionID(a.getWorkflowID(), a.getID(),iteration);
		subAction.setUUID(uid);
		ProcessVariables clonedVars=vars.copy();
		clonedVars.put(PV_KEY_ITERATION,iteration);
		subAction.getProcessingContext().put(ProcessVariables.class,clonedVars);
		subAction.getProcessingContext().put(PV_KEY_ITERATION,iteration);
		subAction.setParentActionID(action.getUUID());
		a.setStatus(ActivityStatus.RUNNING);
		manager.addInternalAction(subAction);
		return subAction.getUUID();
	}
	/**
	 * check whether the maximum number of activities per group is exceeded <br/>
	 * 
	 * TODO allow to specify max number using a property on the activity group
	 *
	 * @throws ExecutionException - thrown if too many activities for the current group
	 */
	protected void incrementCounterAndCheckMaxActivities()throws ExecutionException{
		int ac=getActivityCounter().incrementAndGet();
		WorkflowProperties wp = xnjs.get(WorkflowProperties.class);
		Integer maxProp=wp.getIntValue(WorkflowProperties.MAX_ACTIVITIES_PER_GROUP);
		if(ac>maxProp){
			throw new ExecutionException("Maximum number <"+maxProp+"> of activities per group exceeded!"); 
		}
	}
	
	protected ActivityCounter getActivityCounter(){
		ActivityCounter activityCounter=action.getProcessingContext().get(ActivityCounter.class);
		if(activityCounter==null){
			activityCounter=new ActivityCounter();
			action.getProcessingContext().put(ActivityCounter.class,activityCounter);
		}
		return activityCounter;
	}
	
	/**
	 * perform clean-up after a sub-action has been finished
	 * <ul>
	 *  <li>Modified variables from the sub's processing context are copied into the parent
	 *  <li>the sub-action is destroyed
	 *  <li>the "success" result is stored in the WorkflowInfo persistence
	 * </ul>
	 * @param sub - the sub-action
	 * @throws ExecutionException
	 * @throws ProcessingException
	 * @throws PersistenceException
	 * @throws InterruptedException
	 */
	protected void cleanupSubAction(Action sub)
	throws ExecutionException, ProcessingException, PersistenceException, InterruptedException{
		//copy results
		ProcessVariables pv=sub.getProcessingContext().get(ProcessVariables.class);
		if(pv!=null){
			ProcessVariables myVars = action.getProcessingContext().get(ProcessVariables.class);
			for(String var: pv.getModifiedVariableNames()){
				// TODO scope?
				myVars.put(var, pv.get(var));
				myVars.markModified(var);
			}
		}
		
		String subActivityID=((Activity)sub.getAjd()).getID();
		String subActionID=sub.getUUID();

		// collect statistics from sub-activity
		Statistics subStats=sub.getProcessingContext().get(Statistics.class);
		getStatistics().addAll(subStats);
	
		//clean up the sub-action
		if(!PEConfig.getInstance().isKeepAllActions()){
			xnjs.get(Manager.class).destroy(subActionID, action.getClient());
		}

		WorkflowContainer workflowInfo=null;
		SubflowContainer attr;
		ActivityContainer ag=(ActivityContainer)action.getAjd();
		//store activity status to global workflow info
		try{
			workflowInfo=PEConfig.getInstance().getPersistence().getForUpdate(ag.getWorkflowID());
			attr=workflowInfo.findSubFlowAttributes(ag.getID());
			String iteration=(String)sub.getProcessingContext().get(PV_KEY_ITERATION);
			PEStatus stat=attr.getActivityStatus(subActivityID, iteration);
			stat.setActivityStatus(ActivityStatus.SUCCESS);
		}finally{
			if(workflowInfo!=null){
				try{
					PEConfig.getInstance().getPersistence().write(workflowInfo);
				}catch(Exception ex){
					throw new ProcessingException(ex);
				}
			}
		}
	}
		
	
	/**
	 * create new variables as declared for the given ActivityContainer
	 * 
	 * TODO define scope
	 * 
	 * @param ag - the activity container
	 * @param vars - the process variables
	 */
	protected void addDeclarations(ActivityContainer ag, ProcessVariables vars){
		for(DeclareVariableActivity activity: ag.getDeclarations()){
			try{
				vars.put(activity.getVariableName(), VariableUtil.create(activity.getVariableType(), activity.getInitialValue()));
			}catch(Exception ex){
				String msg="Error processing declaration: "+activity.getVariableName();
				setToDoneAndFailed(msg);
			}
		}
	}
	
	/**
	 * TODO rule engine callout
	 * 
	 * @param sub
	 */
	protected boolean shouldIgnoreFailure(Action sub){
		return false;
	}

	@Override
	protected void setToDoneSuccessfully(){
		action.addLogTrace("All iterations processed.");
		logger.info(action.getAjd().getClass().getSimpleName()+" "+action.getUUID()+": All iterations processed.");
		super.setToDoneSuccessfully();
	}
}
