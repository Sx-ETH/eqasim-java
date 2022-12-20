package org.eqasim.switzerland.drt.travel_times;

import org.eqasim.switzerland.drt.SimulationParameter;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtWaitTimeGlobal;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtWaitTimes;
import org.matsim.contrib.drt.routing.DrtRoute;

public class TravelTimeUpdates {
    private final SimulationParameter simulationParams;
    private WayneCountyDrtZonalSystem zones;
    private final DrtWaitTimes drtWaitTimes;
    private final DrtWaitTimeGlobal drtWaitTimeGlobal;

    public TravelTimeUpdates(SimulationParameter simulationParams, WayneCountyDrtZonalSystem zones, DrtWaitTimes drtWaitTimes, DrtWaitTimeGlobal drtWaitTimeGlobal) {
        this.simulationParams = simulationParams;
        this.zones = zones;
        this.drtWaitTimes = drtWaitTimes;
        this.drtWaitTimeGlobal = drtWaitTimeGlobal;
    }

    public void test(){


        boolean useAverageWaitTime = simulationParams.isUseAverageWaitTime();

        //TravelTimeData globalWaitTime = drtWaitTimeGlobal.getAvgWaitTime();


        //receive avg, median, 90%
       //select the min(avg, median, 90%) for optimistic scenario
       //select the max(persimistic scenario)
       //select the avg()
       //select the median()

       //make it so that drt detour and wait time information can be gotten from here to use in the predictor.
       // Travel time is the max travel time set based on alpha*tt + beta (not actual)
       // same for wait time which just uses maximum setting
       // use global wait time for overall testing
       //if global one is more
   /* travelTime_min = route.getMaxTravelTime() / 60.0;
    //route.getDirectRideTime() * delayFactor
    waitingTime_min = route.getMaxWaitTime() / 60.0;

    // Todo drt wait time and travel time update

				if (useAverageWaitTime) {
        Id<Link> startLinkId = leg.getRoute().getStartLinkId();
        String zone = this.zones.getZoneForLinkId(startLinkId);

        if (drtWaitTimes.getAvgWaitTimes().get(zone) != null) {
            int index = DrtTimeUtils.getTimeBin(leg.getDepartureTime().seconds());
            try {
                waitingTime_min = this.drtWaitTimes.getAvgWaitTimes().get(zone)[index] / 60;
            } catch (IndexOutOfBoundsException e) {
                log.warn(person.getId().toString() + " departs at " + leg.getDepartureTime().seconds());
                waitingTime_min = this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0;
            }
            if (Double.isNaN(waitingTime_min)) {
                waitingTime_min = this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0;
            }
        } else {
            waitingTime_min = this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0;
        }
    }*/}


    public double getTravelTime(DrtRoute route) {
        return 0.0;
    }

    public double getWaitTime(DrtRoute route) {
        return 0.0;
    }
}
