package org.eqasim.switzerland.drt.wait_time;


import com.google.inject.Inject;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.HashMap;
import java.util.Map;

public class DrtWaitTimes implements IterationEndsListener {

    private final WaitTimeTracker trackedWaitTimes;
    private Map<String, double[]> avgWaitTimes;
    WayneCountyDrtZonalSystem zones;

    @Inject
    public DrtWaitTimes(WaitTimeTracker trackedWaitTimes, WayneCountyDrtZonalSystem zones, Config config){

        this.trackedWaitTimes = trackedWaitTimes;
        this.avgWaitTimes = new HashMap<>();
        this.zones = zones;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        //generate wait times for use.
        //this.avgWaitTimes = WaitTimeMetrics.calculateZonalAverageWaitTimes(trackedWaitTimes, zones);

        //toDo if...then configuration for what method to use - define moving window
        //this.avgWaitTimes = WaitTimeMetrics.calculateMovingZonalAverageWaitTimes(trackedWaitTimes, zones, event.getIteration(), 0);

        //test different weights to know the best
        this.avgWaitTimes = WaitTimeMetrics.calculateMethodOfSuccessiveAverageWaitTimes(trackedWaitTimes, zones, event.getIteration(), 0.1);
    }

    public Map<String, double[]> getAvgWaitTimes() {

        return avgWaitTimes;
    }
}
