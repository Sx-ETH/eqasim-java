package org.eqasim.switzerland.drt.travel_times.detour_time;

import org.eqasim.switzerland.drt.travel_times.wait_time.DrtTripData;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DelayFactorMetric {

    private static final Map<Integer,Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
    private static final Map<Integer,Double> iterationsSuccessiveAvg = new HashMap<>();

    public static double calculateGlobalDelayFactor(Set<DrtTripData> drtTrips) {
        double sumTotalTravelTime = 0.0;
        double sumUnsharedTime = 0.0;
        boolean useRouterUnsharedTime = true;   // may remove later when decided on whether to use router or estimate

        for (DrtTripData drtTrip : drtTrips) {
            sumTotalTravelTime =+ drtTrip.totalTravelTime;
            if (useRouterUnsharedTime){
                sumUnsharedTime =+ drtTrip.routerUnsharedTime;
            } else {
                sumUnsharedTime =+ drtTrip.estimatedUnsharedTime;
            }

        }

        //what happens if no drt trip occurred in an iteration
        //division by 0 gives nan/inf and is returned

        //compute delay factor
        return sumTotalTravelTime/sumUnsharedTime;

    }
    public static double calculateGlobalMovingDelayFactor(Set<DrtTripData> drtTrips, int iteration, int movingWindow) {
        Set<DrtTripData> iterDrtTrips = new HashSet<>();
        Set<DrtTripData> allDrtTrips = new HashSet<>();

        iterDrtTrips.addAll(drtTrips);

        //update for next iteration

        iterationsDrtTrips.put(iteration, iterDrtTrips);

        //define starting window
        int start = 0;
        if (iteration > 0 && iteration >= movingWindow) {
            start = iteration - movingWindow + 1;
        }

        for (int i = start; i<=iteration; i++){
            allDrtTrips.addAll(iterationsDrtTrips.get(i));
        }

        return calculateGlobalDelayFactor(allDrtTrips);

    }

    public static double calculateGlobalMethodOfSuccessiveDelayFactor(Set<DrtTripData> drtTrips, int iteration, double weight) {
        double iterationAvg = calculateGlobalDelayFactor(drtTrips);
        double previousAvg = iterationsSuccessiveAvg.get(iteration - 1);

        //what case can we have a null value when it is not iteration 0?
        //no drt was chosen as a mode in that iteration and previous iteration has a value?
        if (iteration == 0 || Double.isNaN(previousAvg) ) {
            iterationsSuccessiveAvg.put(iteration, iterationAvg);
            return iterationAvg;
        }

        //corner case if nan in current, keep most recent value
        if (Double.isNaN(iterationAvg)){
            iterationsSuccessiveAvg.put(iteration, previousAvg);
            return previousAvg;
        }

        double newAvg = (1 - weight) * previousAvg + weight * iterationAvg;
        iterationsSuccessiveAvg.put(iteration, newAvg);

        return newAvg;

    }

    public static void writeDelayStats(Set<DrtTripData> drtTrips, int iterationNumber, Config config) throws IOException {
        String delimiter = ",";
        String filename = "drt_DelayFactorStats.csv";
        String outputDir = config.controler().getOutputDirectory() + "/ITERS/it."+ iterationNumber +"/" + iterationNumber + "." + filename;
        BufferedWriter bw = IOUtils.getBufferedWriter(outputDir);
        bw.write("personId" + delimiter + "startTime" + delimiter + "totalTravelTime" + delimiter +
                "routerUnsharedTime" + delimiter + "estimatedUnsharedTime" + delimiter + "delayFactor" + delimiter +
                "startX"+ delimiter +"startY"+ delimiter +"endX"+ delimiter +"endY");

        for (DrtTripData drtTrip : drtTrips) {
            //print out to csv, the estimatedUnsharedTime from requestSubmissionEvent
            // and compare with this computed one from the route
            bw.newLine();

            bw.write(drtTrip.personId + delimiter +
                    drtTrip.startTime + delimiter +
                    drtTrip.totalTravelTime + delimiter +
                    drtTrip.routerUnsharedTime + delimiter +
                    drtTrip.estimatedUnsharedTime + delimiter +
                    drtTrip.totalTravelTime/drtTrip.routerUnsharedTime + delimiter +
                    drtTrip.startCoord.getX() + delimiter +
                    drtTrip.startCoord.getY() + delimiter +
                    drtTrip.endCoord.getX() + delimiter +
                    drtTrip.endCoord.getY()
            );


        }

        bw.flush();
        bw.close();
    }

}
