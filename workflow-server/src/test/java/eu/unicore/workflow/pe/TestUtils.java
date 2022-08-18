package eu.unicore.workflow.pe;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.unicore.workflow.pe.util.WorkAssignmentUtils;

public class TestUtils {

	private static final String SEP=WorkAssignmentUtils.SEP;
	
	@Test
	public void testEncodeAndDecodeWorkassignmentInformation(){
		String wfID="wf";
		String actID="activity";
		String iteration="iteration";
		String submission="sub1";
		String waID=WorkAssignmentUtils.getEncodedWorkAssignmentID(wfID, actID, 
				iteration, submission);
		assert ("wf"+SEP+"activity"+SEP+"iteration"+SEP+"sub1").equals(waID);
		String wf_ext=WorkAssignmentUtils.getWorkflowIDFromEncodedWAID(waID);
		assert wfID.equals(wf_ext);
		
		String submission_ext=WorkAssignmentUtils.getSubmissionCounterFromEncodedWAID(waID);
		assert submission.equals(submission_ext);
		
		String iteration_ext=WorkAssignmentUtils.getIterationFromEncodedWAID(waID);
		assert iteration.equals(iteration_ext);
		
		String actID_ext=WorkAssignmentUtils.getActivityIDFromEncodedWAID(waID);
		assert "activity".equals(actID_ext);
		
		String actionID_ext=WorkAssignmentUtils.getActionIDFromEncodedWAID(waID);
		assert ("wf"+SEP+"activity"+SEP+"iteration").equals(actionID_ext);
		
	} 
	

	@Test
	public void testExtractActionID()throws Exception{
		String uid=WorkAssignmentUtils.getActionIDFromEncodedWAID("1/2/3/cutthis");
		assert uid.equals("1/2/3");
	}

	@Test
	public void testBlacklist() throws Exception {
		List<String> l = new ArrayList<>();
		String existing = "";
		l.add("http://foo");
		l.add("http://bar");
		String csv = WorkAssignmentUtils.toCommaSeparatedList(l, existing);
		assert "http://foo,http://bar".equals(csv);
		l.clear();
		l.add("http://micros0ft.com");
		csv = WorkAssignmentUtils.toCommaSeparatedList(l, csv);
		assert "http://foo,http://bar,http://micros0ft.com".equals(csv);
	}
	
}
