package eu.unicore.workflow.xnjs;

import java.util.Calendar;
import java.util.Map;

import com.google.inject.Inject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import eu.unicore.security.Client;

public class MockReservation implements IReservation {
	
	@Inject
	public MockReservation(XNJS configuration){}
	
	public void cancelReservation(String resID, Client arg1)
	throws ExecutionException {}

	public String makeReservation(Map<String,String> resources, Calendar startTime, Client client)
	throws ExecutionException {
		return "123";
	}
	
	public ReservationStatus queryReservation(String resID,Client arg2)
	throws ExecutionException {
		return new ReservationStatus();
	}

}
