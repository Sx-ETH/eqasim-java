package org.eqasim.switzerland.drt.travel_times;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;

public class DrtGlobalMetrics {
	private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
	private static final Map<Integer, TravelTimeData> iterationsSuccessiveAvg = new HashMap<>();

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
//	public static double calculateAveragelDelayFactor(Set<DrtTripData> drtTrips) {
//		double sumTotalTravelTime = 0.0;
//		double sumUnsharedTime = 0.0;
//		boolean useRouterUnsharedTime = true; // may remove later when decided on whether to use router or estimate
//
//		for (DrtTripData drtTrip : drtTrips) {
//			sumTotalTravelTime += drtTrip.totalTravelTime;
//			if (useRouterUnsharedTime) {
//				sumUnsharedTime += drtTrip.routerUnsharedTime;
//			} else {
//				sumUnsharedTime += drtTrip.estimatedUnsharedTime;
//			}
//
//		}
//
//		// what happens if no drt trip occurred in an iteration
//		// division by 0 gives nan/inf and is returned
//
//		// compute delay factor
//		// TODO: Is this really what we want? The avg of the delay factor is different
//		// than this -> not sure which one should we use
//		return sumTotalTravelTime / sumUnsharedTime; // toDo would we have aggregation error?
//		// this method smoothens the delay factor, giving more weights to the longer
//		// trips, other method gives everyone equal factor
//
//	}

	public static double[] collectWaitTimes(Set<DrtTripData> drtTrips) {
		double[] l = new double[drtTrips.size()];

		int i = 0;
		for (DrtTripData drtTrip : drtTrips) {
			l[i] = drtTrip.waitTime;
			++i;
		}
		return l;
	}

	public static double[] collectDelayFactors(Set<DrtTripData> drtTrips) {
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

	public static TravelTimeData calculateGlobalMetrics(Set<DrtTripData> drtTrips) {
		double[] waitTimes = collectWaitTimes(drtTrips);
		double[] delayFactors = collectDelayFactors(drtTrips);
		TravelTimeData travelTimeData = new TravelTimeData(waitTimes, delayFactors);

		return travelTimeData;

	}

	public static TravelTimeData calculateGlobalMovingMetrics(Set<DrtTripData> drtTrips, int iteration,
			int movingWindow) {
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

		return calculateGlobalMetrics(allDrtTrips);

	}

	public static TravelTimeData calculateMethodOfSuccessiveAverageWaitTime(Set<DrtTripData> drtTrips, int iteration,
			double weight) {

		TravelTimeData iterationTimeData = calculateGlobalMetrics(drtTrips);

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

	public static void writeDrtTripsStats(Set<DrtTripData> drtTrips, int iterationNumber, Config config)
			throws IOException {
		String delimiter = ",";
		String filename = "drt_drtTripsStats.csv";
		String outputDir = config.controler().getOutputDirectory() + "/ITERS/it." + iterationNumber + "/"
				+ iterationNumber + "." + filename;
		BufferedWriter bw = IOUtils.getBufferedWriter(outputDir);
		bw.write("personId" + delimiter + "startTime" + delimiter + "totalTravelTime" + delimiter + "routerUnsharedTime"
				+ delimiter + "estimatedUnsharedTime" + delimiter + "delayFactor" + delimiter + "waitTime" + delimiter
				+ "startX" + delimiter + "startY" + delimiter + "endX" + delimiter + "endY");

		for (DrtTripData drtTrip : drtTrips) {
			// print out to csv, the estimatedUnsharedTime from requestSubmissionEvent
			// and compare with this computed one from the route
			bw.newLine();

			double delayFactor;
			if (drtTrip.routerUnsharedTime > 0) {
				delayFactor = drtTrip.totalTravelTime / drtTrip.routerUnsharedTime;
			} else {
				delayFactor = drtTrip.totalTravelTime / drtTrip.estimatedUnsharedTime;
			}

			bw.write(drtTrip.personId + delimiter + drtTrip.startTime + delimiter + drtTrip.totalTravelTime + delimiter
					+ drtTrip.routerUnsharedTime + delimiter + drtTrip.estimatedUnsharedTime + delimiter + delayFactor
					+ delimiter + drtTrip.waitTime + delimiter + drtTrip.startCoord.getX() + delimiter
					+ drtTrip.startCoord.getY() + delimiter + drtTrip.endCoord.getX() + delimiter
					+ drtTrip.endCoord.getY());

		}

		bw.flush();
		bw.close();
	}

}
