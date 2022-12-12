package org.eqasim.switzerland.drt.travel_times.detour_time;


import com.google.inject.Inject;
import org.eqasim.switzerland.drt.travel_times.DrtTimeTracker;
import org.eqasim.switzerland.drt.travel_times.DrtTripData;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.IOException;
import java.util.Set;

public class DrtDetourTimes implements IterationEndsListener {

    private final DrtTimeTracker trackedTimes;

    private double globalDelayFactor = 0.0;


    Config config;

    @Inject
    public DrtDetourTimes(DrtTimeTracker trackedTimes, Config config){

        this.trackedTimes = trackedTimes;
        this.config = config;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
       //global delay factor for different methods
        String method = "global"; //ToDo set the method via the config
        double weight = 0.5; //ToDo set the weight in the config
        int movingWindow = 5; //ToDo set in config
        Set<DrtTripData> drtTrips = this.trackedTimes.getDrtTrips();
        switch(method){
            case "global":
                this.globalDelayFactor = DelayFactorMetric.calculateGlobalDelayFactor(drtTrips);

                break;
            case "moving":
                this.globalDelayFactor = DelayFactorMetric.calculateGlobalMovingDelayFactor(drtTrips, event.getIteration(), movingWindow);
                break;
            case "successive":
                this.globalDelayFactor = DelayFactorMetric.calculateGlobalMethodOfSuccessiveDelayFactor(drtTrips, event.getIteration(), weight);
                break;
            default:
                throw new IllegalArgumentException(
                        "Method for computing drt delay times should be one of these: [global, moving, successive]");

        }
//ToDo: include a condition whether to write out the detailed delay stats per iteration or not or have a separate class for analysis
        //write out delay metrics per iteration
        try {
            DelayFactorMetric.writeDelayStats(trackedTimes.getDrtTrips(), event.getIteration(), config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public double getGlobalDelayFactor() {

        return globalDelayFactor;
    }
}
