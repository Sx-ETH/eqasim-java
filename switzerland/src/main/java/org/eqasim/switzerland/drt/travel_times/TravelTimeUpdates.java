package org.eqasim.switzerland.drt.travel_times;

import com.google.inject.Inject;
import org.eqasim.switzerland.drt.SimulationParameter;
import org.eqasim.switzerland.drt.travel_times.zonal.SquareGridDrtZonalSystem;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.IOException;
import java.util.Set;

public class TravelTimeUpdates implements IterationEndsListener {
    private final SimulationParameter simulationParams;
    private SquareGridDrtZonalSystem zones;
    private final DrtTimeTracker trackedTimes;
    private TravelTimeData travelTimeData; // all global wait and delay stats are stored here
    private Config config;
    private DrtPredictions drtPredictions;

    @Inject
    public TravelTimeUpdates(SimulationParameter simulationParams, SquareGridDrtZonalSystem zones,
                             DrtTimeTracker trackedTimes, Config config, DrtPredictions drtPredictions) {
        this.simulationParams = simulationParams;
        this.zones = zones;
        this.trackedTimes = trackedTimes;
        this.config = config;
        this.drtPredictions = drtPredictions;
    }

    public double getTravelTime_Sec(DrtRoute route) {
        if (simulationParams.isUseDelayFactor()) {
            if (simulationParams.getDelayFactorFeedback().equals("average")
                    && !Double.isNaN(travelTimeData.delayFactorStat.avg)) {
                return route.getDirectRideTime() * travelTimeData.delayFactorStat.avg;
            }

            if (simulationParams.getDelayFactorFeedback().equals("median")
                    && !Double.isNaN(travelTimeData.delayFactorStat.median)) {
                return route.getDirectRideTime() * travelTimeData.delayFactorStat.median;
            }
            if (simulationParams.getDelayFactorFeedback().equals("divisionOfSums")
                    && !Double.isNaN(travelTimeData.delayFactorStat.avg)) {
                return route.getDirectRideTime() * travelTimeData.delayFactorStat.avg;
            }
            // toDofor zone
        }
        return route.getMaxTravelTime(); // toDo now alpha*time + beta, alpa and beta defined in drt config module
    }

    public double getWaitTime_Sec(DrtRoute route) {
        if (simulationParams.isUseAverageWaitTime()) {
            if (simulationParams.getWaitTimeFeedback().equals("average")
                    && !Double.isNaN(travelTimeData.waitTimeStat.avg)) {
                return travelTimeData.waitTimeStat.avg;
            }
            if (simulationParams.getWaitTimeFeedback().equals("median")
                    && !Double.isNaN(travelTimeData.waitTimeStat.median)) {
                return travelTimeData.waitTimeStat.median;
            }
        }
        return route.getMaxWaitTime();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // TODO: Same method for both calculations, should we allow it to be different?
        String delayMethod = this.simulationParams.getDelayCalcMethod();
        String waitMethod = this.simulationParams.getWaitTimeMethod();
        Boolean isGlobalMethod = !delayMethod.contains("zonal");
        String method = isGlobalMethod ? waitMethod : null; // toDo define if null use zonal

        double weight = this.simulationParams.getMsaWeight();
        int movingWindow = this.simulationParams.getMovingWindow();
        Set<DrtTripData> drtTrips = this.trackedTimes.getDrtTrips();

        // TODO: Only compute the needed one -> this is to test that it works correctly
        TravelTimeData globalAvg = DrtGlobalMetrics.calculateGlobalMetrics(drtTrips,
                this.simulationParams.getDelayFactorFeedback());
        String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                "drt_travelTimeData_global.csv");
        globalAvg.write(fileName);

        TravelTimeData movingAvg = DrtGlobalMetrics.calculateGlobalMovingMetrics(drtTrips, event.getIteration(),
                movingWindow, this.simulationParams.getDelayFactorFeedback());
        fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                "drt_travelTimeData_moving.csv");
        movingAvg.write(fileName);

        TravelTimeData successiveAvg = DrtGlobalMetrics.calculateMethodOfSuccessiveAverageWaitTime(drtTrips,
                event.getIteration(), weight, this.simulationParams.getDelayFactorFeedback());
        fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                "drt_travelTimeData_successive.csv");
        successiveAvg.write(fileName);

        switch (method) {
            case "global":
                this.travelTimeData = globalAvg;
                break;
            case "moving":
                this.travelTimeData = movingAvg;
                break;
            case "successive":
                this.travelTimeData = successiveAvg;
                break;
            default:
                throw new IllegalArgumentException("Method not implemented yet!"); // toDo catch the null
        }

        try {
            DrtGlobalMetrics.writeDrtTripsStats(drtTrips, event.getIteration(), this.config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.drtPredictions.writeTripsPredictions(event.getIteration(), this.config);
        this.drtPredictions.clearTripPredictions();

    }

    public void zonalStuff() {

        boolean useZonalAverageWaitTime = true; // toDo cmd config should say to use avg wait time or not

        // receive avg, median, 90%
        // select the min(avg, median, 90%) for optimistic scenario
        // select the max(persimistic scenario)
        // select the avg()
        // select the median()

        // make it so that drt detour and wait time information can be gotten from here
        // to use in the predictor.
        // Travel time is the max travel time set based on alpha*tt + beta (not actual)
        // same for wait time which just uses maximum setting
        // use global wait time for overall testing
        // if global one is more
        /*
         * travelTime_min = route.getMaxTravelTime() / 60.0; //route.getDirectRideTime()
         * * delayFactor waitingTime_min = route.getMaxWaitTime() / 60.0;
         *
         * // Todo drt wait time and travel time update
         *
         * if (useAverageWaitTime) { Id<Link> startLinkId =
         * leg.getRoute().getStartLinkId(); String zone =
         * this.zones.getZoneForLinkId(startLinkId);
         *
         * if (drtWaitTimes.getAvgWaitTimes().get(zone) != null) { int index =
         * DrtTimeUtils.getTimeBin(leg.getDepartureTime().seconds()); try {
         * waitingTime_min = this.drtWaitTimes.getAvgWaitTimes().get(zone)[index] / 60;
         * } catch (IndexOutOfBoundsException e) { log.warn(person.getId().toString() +
         * " departs at " + leg.getDepartureTime().seconds()); waitingTime_min =
         * this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0; } if
         * (Double.isNaN(waitingTime_min)) { waitingTime_min =
         * this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0; } } else { waitingTime_min =
         * this.drtWaitTimeGlobal.getAvgWaitTime() / 60.0; } }
         */
    }

}
