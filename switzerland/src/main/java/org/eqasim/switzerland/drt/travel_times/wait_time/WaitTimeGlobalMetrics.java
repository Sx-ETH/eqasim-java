package org.eqasim.switzerland.drt.travel_times.wait_time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.switzerland.drt.travel_times.DrtTripData;

public class WaitTimeGlobalMetrics {
	// Computes the waitTime average for all the drt trips in the previous iteration
	// or iterations (depending on the mode)
	private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
	private static final Map<Integer, Double> iterationsSuccessiveAvg = new HashMap<>();

	public static double calculateAverageWaitTime(Set<DrtTripData> drtTrips) {
		double average = 0.0;
		int observations = 0;

		for (DrtTripData drtTrip : drtTrips) {
			if (!drtTrip.rejected) {
				average += drtTrip.waitTime;
				observations += 1;
			}
		}
		average = average / observations;

		return average;
	}

	public static double calculateMovingAverageWaitTime(Set<DrtTripData> drtTrips, int iteration, int movingWindow) {
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

		// Find total averages for the time period
		return calculateAverageWaitTime(allDrtTrips);

	}

	public static double calculateMethodOfSuccessiveAverageWaitTime(Set<DrtTripData> drtTrips, int iteration,
			double weight) {
		double iterationAvg = calculateAverageWaitTime(drtTrips);
		if (iteration == 0) {
			iterationsSuccessiveAvg.put(iteration, iterationAvg);
			return iterationAvg;
		}
		double previousAvg = iterationsSuccessiveAvg.get(iteration - 1);

		// In the case the previous avg is Nan then we only use the current average
		if (Double.isNaN(previousAvg)) {
			iterationsSuccessiveAvg.put(iteration, iterationAvg);
			return iterationAvg;
		}
		// In the case there are no trips in this iteration then we keep the value
		// estimated in the previous iteration (so we'll never have nans after one
		// correct value)
		double newAvg;
		if (Double.isNaN(iterationAvg)) {
			newAvg = previousAvg;
		} else {
			newAvg = (1 - weight) * previousAvg + weight * iterationAvg;
		}
		iterationsSuccessiveAvg.put(iteration, newAvg);

		return newAvg;

	}

}
