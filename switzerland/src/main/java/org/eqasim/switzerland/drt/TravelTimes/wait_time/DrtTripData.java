package org.eqasim.switzerland.drt.TravelTimes.wait_time;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.Map;


public class DrtTripData {
    public Id<Request> requestId;

    public Id<Person> personId;
    public Id<Link> startLinkId;

    public Coord startCoord;
    public Id<Link> endLinkId;

    public Coord endCoord;
    public double startTime;
    public double pickUpTime;
    public double waitTime;

    public Map<Id<Request>, DrtRequestSubmittedEvent> requestEvent;

    public double estimatedUnsharedTime;

    public double routerUnsharedTime;

    public double unsharedDistance;

    public double totalTravelTime;
    public boolean rejected = false;
}
