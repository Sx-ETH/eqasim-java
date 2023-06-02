package org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;

public class PersonLevelRejectionTracker extends RejectionTracker {
	private IdMap<Person, Integer> numberOfRequests = new IdMap<>(Person.class);
	private IdMap<Person, Integer> numberOfRejections = new IdMap<>(Person.class);

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		super.handleEvent(event);
		numberOfRejections.compute(event.getPersonId(), (k, v) -> v == null ? 1 : v + 1);
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		super.handleEvent(event);
		numberOfRequests.compute(event.getPersonId(), (k, v) -> v == null ? 1 : v + 1);
	}

	public double getRejectionProbability(Id<Person> personId) {
		if(!numberOfRequests.containsKey(personId)) {
			return 0;
		}
		return ((double) numberOfRejections.getOrDefault(personId, 0))
				/ numberOfRequests.get(personId);
	}

	public int getNumberOfRejections(Id<Person> personId) {
		return this.numberOfRejections.getOrDefault(personId, 0);
	}

	public int getNumberOfRequests(Id<Person> personId) {
		return this.numberOfRequests.getOrDefault(personId, 0);
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.numberOfRequests.clear();
		this.numberOfRejections.clear();
	}

}
