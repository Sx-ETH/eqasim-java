package org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty;

import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;

public class RejectionTracker implements PassengerRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {
	private Integer numberOfRequests=0;
	private Integer numberOfRejections=0;


	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		this.numberOfRejections++;
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		this.numberOfRequests++;
	}

	public double getRejectionProbability() {
		if(this.numberOfRequests == 0) {
			return 0;
		}
		return ((double)this.numberOfRejections)/this.numberOfRequests;
	}

	public int getNumberOfRequests() {
		return this.numberOfRequests;
	}

	public int getNumberOfRejections() {
		return this.numberOfRejections;
	}

	@Override
	public void reset(int iteration) {
		this.numberOfRejections = 0;
		this.numberOfRequests = 0;
	}
}
