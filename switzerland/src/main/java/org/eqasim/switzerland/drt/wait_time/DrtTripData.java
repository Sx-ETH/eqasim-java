package org.eqasim.switzerland.drt.wait_time;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public class DrtTripData {
    public Id<Link> startLinkId;
    public double startTime;
    public double pickUpTime;
    public double waitTime;

    public double unsharedTime;

    public double totalTravelTime;
    public boolean rejected = false;
}
