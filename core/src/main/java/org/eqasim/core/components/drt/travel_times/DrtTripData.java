package org.eqasim.core.components.drt.travel_times;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;


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

    public double estimatedUnsharedTime;

    public double routerUnsharedTime;

    public double unsharedDistance;

    public double totalTravelTime;
    public double arrivalTime;
    public boolean rejected = false;

    @Override
    public String toString() {
        String result = "startLinkId = " + startLinkId;
        result += "\nstartTime = " + String.valueOf(startTime);
        result += "\npickUpTime = " + String.valueOf(pickUpTime);
        result += "\nwaitTime = " + String.valueOf(waitTime);
        result += "\nunsharedTime = " + String.valueOf(routerUnsharedTime);
        result += "\nestimatedUnsharedTime = " + String.valueOf(estimatedUnsharedTime);
        result += "\ntotalTravelTime = " + String.valueOf(totalTravelTime);
        result += "\nrejected = " + String.valueOf(rejected);
        return result;
    }
}
