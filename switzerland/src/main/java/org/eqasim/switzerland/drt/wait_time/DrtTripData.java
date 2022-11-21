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
    
    @Override
    public String toString() {
    	String result = "starLinkId = " + startLinkId;
    	result += "\nstartTime = " + String.valueOf(startTime);
    	result += "\npickUpTime = " + String.valueOf(pickUpTime);
    	result += "\nwaitTime = " + String.valueOf(waitTime);
    	result += "\nunsharedTime = " + String.valueOf(unsharedTime);
    	result += "\ntotalTravelTime = " + String.valueOf(totalTravelTime);
    	result += "\nrejected = " + String.valueOf(rejected);
    	return result;
    }
}
