package org.eqasim.switzerland.drt.travel_times.wait_time;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;


public class WaitTimeMetrics {
    private static final Map<Integer, DrtTimeTracker> dailyWaitTimes = new HashMap<>();
    private static Map<String, double[]> dailyAverageWaitTimes = new HashMap<>();

    private static TripRouter tripRouter;


    private static Scenario scenario;

    @Inject
    public WaitTimeMetrics(TripRouter tripRouter, Scenario scenario) {
        this.tripRouter = tripRouter;
        this.scenario = scenario;
    }

    private static Map<String, Set<WaitTimeData>> createZonalStats(DrtTimeTracker waitTimes, WayneCountyDrtZonalSystem zones, Map<String, Set<WaitTimeData>> zonalWaitTimes) {
        Set<DrtTripData> drtTrips = waitTimes.getDrtTrips();

        for (DrtTripData drtTrip : drtTrips) {

            String zone = zones.getZoneForLinkId(drtTrip.startLinkId);
            if (zonalWaitTimes.containsKey(zone)) {
                WaitTimeData wtd = new WaitTimeData();
                wtd.startTime = drtTrip.startTime;
                wtd.waitTime = drtTrip.waitTime;

                zonalWaitTimes.get(zone).add(wtd);
            } else {

                Set<WaitTimeData> newWaitingTimes = new HashSet<>();
                WaitTimeData wtd = new WaitTimeData();
                wtd.startTime = drtTrip.startTime;
                wtd.waitTime = drtTrip.waitTime;
                newWaitingTimes.add(wtd);
                zonalWaitTimes.put(zone, newWaitingTimes);
            }
        }

        return zonalWaitTimes;

    }

    public static Map<String, double[]> calculateZonalAverageWaitTimes(DrtTimeTracker waitTimes, WayneCountyDrtZonalSystem zones) {
        Map<String, Set<WaitTimeData>> zonalWaitTimes = createZonalStats(waitTimes, zones, new HashMap<>());
        Map<String, double[]> avgZonalWaitTimes = new HashMap<>();
        int timeBins = 100; //toDo justify the choice of this time bin now it is hourly and set at 100 to capture multiday trips

        for (String zone : zonalWaitTimes.keySet()) {
            double[] average = new double[timeBins];
            int[] observations = new int[timeBins];

            for (WaitTimeData d : zonalWaitTimes.get(zone)) {

                int index = ((int) (d.startTime / 3600.0));
                average[index] += d.waitTime;
                observations[index]++;

            }
            for (int i = 0; i < average.length; i++) {
                if (observations[i] > 0)
                    average[i] = average[i] / observations[i];
            }

            avgZonalWaitTimes.put(zone, average);
        }
        return avgZonalWaitTimes;
    }

    public static Map<String, double[]> calculateMovingZonalAverageWaitTimes(DrtTimeTracker waitTimes, WayneCountyDrtZonalSystem zones, int iteration, int movingWindow) {

        Set<DrtTripData> newDrtTrips = waitTimes.getDrtTrips();

        //define starting window  //iteration starts from zero so subtract 1 from movingWindow
        int start = 0;
        if (iteration > 0 && iteration > movingWindow) {
            start = iteration - movingWindow - 1;
        }
//        if (iteration == 0) {
//            dailyWaitTimes.put(iteration, waitTimes);
//        }

        //add past wait times from the starting window
        if(iteration != 0) {
            for (int i = start; i<iteration; i++){
                newDrtTrips.addAll(dailyWaitTimes.get(i).getDrtTrips());
            }
        }

        //update with current wait times of the day
        dailyWaitTimes.put(iteration, waitTimes);

        //get all the wait times and combine per iteration
        DrtTimeTracker updatedWaitTimes = new DrtTimeTracker(tripRouter, scenario);  //ToDo confirm if this is injected correctly
        updatedWaitTimes.setDrtTrips(newDrtTrips);

        //Find total averages for the time period
        return calculateZonalAverageWaitTimes(updatedWaitTimes, zones);
    }

    public static Map<String, double[]> calculateMethodOfSuccessiveAverageWaitTimes(DrtTimeTracker waitTimes, WayneCountyDrtZonalSystem zones, int iteration, double weight) {

        //avgwaitTime = (1-phi)V_prev_iter + phi*V_iter where V are wait time averages of past or current iterations
        if (iteration == 0) {
            dailyAverageWaitTimes = calculateZonalAverageWaitTimes(waitTimes, zones);
        }

        Map<String, double[]> currentAvgwaitTime = calculateZonalAverageWaitTimes(waitTimes, zones);
        currentAvgwaitTime.forEach((k, v) -> {
            dailyAverageWaitTimes.merge(k, v, (v1, v2) -> {
                IntStream.range(0, v1.length).forEach(i -> {
                    v1[i] = (1-weight) * v1[i] + weight * v2[i];
                });
                return v1;
            });
        });

        return dailyAverageWaitTimes;



        //avgwaitTime = (1-phi)V_prev_iter + phi*V_iter where V are wait time averages of past or current iterations
        //v_iter = calculateZonalAverageWaitTimes(waitTimes, zones);

        //for each zone in dict v_iter and for each zone in dict v_prev_iter
        //if they are same zone:
        //do an array sum:
        //for element in the arrays combine based on formula
        //update new array
        //add new array to zone in a new dict

        //update v_prev_iter
        //v_prev_iter = avgwaittime


    }
}
