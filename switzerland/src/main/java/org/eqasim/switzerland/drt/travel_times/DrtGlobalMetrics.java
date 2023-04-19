package org.eqasim.switzerland.drt.travel_times;

public class DrtGlobalMetrics {
    /*
    private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
    private static final Map<Integer, TravelTimeData> iterationsSuccessiveAvg = new HashMap<>();


    private static Logger logger = Logger.getLogger(DrtGlobalMetrics.class);


    //	public static double calculateAverageWaitTime(Set<DrtTripData> drtTrips) {
//		double average = 0.0;
//		int observations = 0;
//
//		for (DrtTripData drtTrip : drtTrips) {
//			if (!drtTrip.rejected) { // TODO: Should this be here or in the TimeTracker we just don't add it?
//				average += drtTrip.waitTime;
//				observations += 1;
//			}
//		}
//		average = average / observations;
//
//		return average;
//	}
//
    private static double calculateAverageDelayFactorFromSums(Set<DrtTripData> drtTrips) {
        double sumTotalTravelTime = 0.0;
        double sumUnsharedTime = 0.0;
        boolean useRouterUnsharedTime = true; // may remove later when decided on whether to use router or estimate

        for (DrtTripData drtTrip : drtTrips) {
            sumTotalTravelTime += drtTrip.totalTravelTime;
            if (useRouterUnsharedTime) {
                sumUnsharedTime += drtTrip.routerUnsharedTime;
            } else {
                sumUnsharedTime += drtTrip.estimatedUnsharedTime;
            }
        }

        // what happens if no drt trip occurred in an iteration
        // division by 0 gives nan/inf and is returned

        // compute delay factor
        return sumTotalTravelTime / sumUnsharedTime;

    }

    private static double[] collectWaitTimes(Set<DrtTripData> drtTrips) {
        double[] l = new double[drtTrips.size()];

        int i = 0;
        for (DrtTripData drtTrip : drtTrips) {
            l[i] = drtTrip.waitTime;
            ++i;
        }
        return l;
    }

    private static double[] collectDelayFactors(Set<DrtTripData> drtTrips) {
        double[] l = new double[drtTrips.size()];
        int i = 0;
        for (DrtTripData drtTrip : drtTrips) {
            if (drtTrip.routerUnsharedTime > 0) {
                l[i] = drtTrip.totalTravelTime / drtTrip.routerUnsharedTime;
            } else {
                l[i] = drtTrip.totalTravelTime / drtTrip.estimatedUnsharedTime;
            }
            ++i;
        }
        return l;
    }

    public static TravelTimeData calculateGlobalMetrics(Set<DrtTripData> drtTrips, String delayFactorMethod) {
        TravelTimeData travelTimeData;

        double[] waitTimes = collectWaitTimes(drtTrips);
        if (delayFactorMethod.equals("divisionOfSums")) {
            double avgDF = calculateAverageDelayFactorFromSums(drtTrips);
            travelTimeData = new TravelTimeData(waitTimes, avgDF);

        } else {
            double[] delayFactors = collectDelayFactors(drtTrips);
            travelTimeData = new TravelTimeData(waitTimes, delayFactors);
        }

        return travelTimeData;

    }

    public static TravelTimeData calculateGlobalMovingMetrics(Set<DrtTripData> drtTrips, int iteration,
                                                              int movingWindow, String delayFactorMethod) {
        Set<DrtTripData> iterationDrtTrips = new HashSet<>();

        // We have to add them to a new set because if not then it would be a reference
        // and the reset after each iteration would delete the trips
        iterationDrtTrips.addAll(drtTrips);
        // update with current drt trips
        iterationsDrtTrips.put(iteration, iterationDrtTrips);

        int start = 0;
        if (iteration > 0 && iteration >= movingWindow) {
            start = iteration - movingWindow + 1;
        }

        Set<DrtTripData> allDrtTrips = new HashSet<>();

        for (int i = start; i <= iteration; i++) {
            allDrtTrips.addAll(iterationsDrtTrips.get(i));

        }

        return calculateGlobalMetrics(allDrtTrips, delayFactorMethod);

    }

    public static TravelTimeData calculateMethodOfSuccessiveAverageWaitTime(Set<DrtTripData> drtTrips, int iteration,
                                                                            double weight, String delayFactorMethod) {

        TravelTimeData iterationTimeData = calculateGlobalMetrics(drtTrips, delayFactorMethod);

        if (iteration == 0) {
            iterationsSuccessiveAvg.put(iteration, iterationTimeData);
            return iterationTimeData;
        }
        TravelTimeData previousIterationData = iterationsSuccessiveAvg.get(iteration - 1);

        TravelTimeData newIterationData = TravelTimeData.combineIterationsSuccessive(previousIterationData,
                iterationTimeData, weight);
        iterationsSuccessiveAvg.put(iteration, newIterationData);
        return newIterationData;

    }
*/

}
