package eu.unicore.workflow.xnjs;

import java.util.Calendar;
import java.util.Map;

import com.google.inject.Inject;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.ReservationStatus;

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
