package org.eqasim.switzerland.drt.travel_times.wait_time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.switzerland.drt.travel_times.DrtTimeUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTripData;
import org.eqasim.switzerland.drt.travel_times.WayneCountyDrtZonalSystem;

public class WaitTimeMetrics {
	private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
	private static final Map<Integer, Map<String, double[]>> iterationsSuccessiveAvg = new HashMap<>();

	private static Map<String, Set<WaitTimeData>> createZonalStats(Set<DrtTripData> drtTrips,
			WayneCountyDrtZonalSystem zones, Map<String, Set<WaitTimeData>> zonalWaitTimes) {

		for (DrtTripData drtTrip : drtTrips) {

			String zone = zones.getZoneForLinkId(drtTrip.startLinkId);
			if (zonalWaitTimes.containsKey(zone)) {
				WaitTimeData wtd = new WaitTimeData();
				wtd.startTime = drtTrip.startTime;
				wtd.waitTime = drtTrip.waitTime;
				wtd.rejected = drtTrip.rejected;

				zonalWaitTimes.get(zone).add(wtd);
			} else {

				Set<WaitTimeData> newWaitingTimes = new HashSet<>();
				WaitTimeData wtd = new WaitTimeData();
				wtd.startTime = drtTrip.startTime;
				wtd.waitTime = drtTrip.waitTime;
				wtd.rejected = drtTrip.rejected;
				newWaitingTimes.add(wtd);
				zonalWaitTimes.put(zone, newWaitingTimes);
			}
		}

		return zonalWaitTimes;

	}

	public static Map<String, double[]> calculateZonalAverageWaitTimes(Set<DrtTripData> drtTrips,
			WayneCountyDrtZonalSystem zones) {
		Map<String, Set<WaitTimeData>> zonalWaitTimes = createZonalStats(drtTrips, zones, new HashMap<>());
		Map<String, double[]> avgZonalWaitTimes = new HashMap<>();
		int timeBins = DrtTimeUtils.getWaitingTimeBinCount(); // toDo justify the choice of this time bin now it is
																// hourly and set at 100 to
		// capture multiday trips

		for (String zone : zonalWaitTimes.keySet()) {
			double[] average = new double[timeBins];
			int[] observations = new int[timeBins];

			for (WaitTimeData d : zonalWaitTimes.get(zone)) {
				if (!d.rejected) {
					int index = DrtTimeUtils.getTimeBin(d.startTime);
					average[index] += d.waitTime;
					observations[index]++;
				}

			}
			for (int i = 0; i < average.length; i++) {
				average[i] = average[i] / observations[i];
			}

			avgZonalWaitTimes.put(zone, average);
		}
		return avgZonalWaitTimes;
	}

	public static Map<String, double[]> calculateMovingZonalAverageWaitTimes(Set<DrtTripData> drtTrips,
			WayneCountyDrtZonalSystem zones, int iteration, int movingWindow) {

		Set<DrtTripData> iterationDrtTrips = new HashSet<>();

		iterationDrtTrips.addAll(drtTrips);
		// update with current drt trips
		iterationsDrtTrips.put(iteration, iterationDrtTrips);

		// define starting window
		int start = 0;
		if (iteration > 0 && iteration >= movingWindow) {
			start = iteration - movingWindow + 1;
		}

		Set<DrtTripData> allDrtTrips = new HashSet<>();

		for (int i = start; i <= iteration; i++) {
			allDrtTrips.addAll(iterationsDrtTrips.get(i));
		}

		// Find total averages for the time period
		return calculateZonalAverageWaitTimes(allDrtTrips, zones);
	}

	public static Map<String, double[]> calculateMethodOfSuccessiveAverageWaitTimes(Set<DrtTripData> drtTrips,
			WayneCountyDrtZonalSystem zones, int iteration, double weight) {
		Map<String, double[]> iterationAvg = calculateZonalAverageWaitTimes(drtTrips, zones);
		Map<String, double[]> successiveItAvg = new HashMap<>();
		int timeBins = DrtTimeUtils.getWaitingTimeBinCount();
		// avgwaitTime = (1-phi)V_prev_iter + phi*V_iter where V are wait time averages
		// of past or current iterations
		if (iteration == 0) {
			iterationsSuccessiveAvg.put(iteration, iterationAvg);
			return iterationAvg;
		}

		Map<String, double[]> previousAvg = iterationsSuccessiveAvg.get(iteration - 1);
		Set<String> allZones = new HashSet<>();
		allZones.addAll(iterationAvg.keySet());
		allZones.addAll(previousAvg.keySet());

		for (String zone : allZones) {
			double[] successiveAvg = new double[timeBins];

			for (int i = 0; i < timeBins; i++) {
				if (iterationAvg.keySet().contains(zone) && previousAvg.keySet().contains(zone)) {
					if (Double.isNaN(previousAvg.get(zone)[i])) {
						successiveAvg[i] = iterationAvg.get(zone)[i];
					}
					// In the case there are no trips in this iteration then we keep the value
					// estimated in the previous iteration (so we'll never have nans after one
					// correct value)
					else if (Double.isNaN(iterationAvg.get(zone)[i])) {
						successiveAvg[i] = previousAvg.get(zone)[i];
					} else {
						successiveAvg[i] = (1 - weight) * previousAvg.get(zone)[i] + weight * iterationAvg.get(zone)[i];
					}

				} else if (iterationAvg.keySet().contains(zone)) {
					successiveAvg[i] = iterationAvg.get(zone)[i];
				} else if (previousAvg.keySet().contains(zone)) {
					successiveAvg[i] = previousAvg.get(zone)[i];
				} else {
					throw new IllegalArgumentException("This should not happen");
				}
			}
			successiveItAvg.put(zone, successiveAvg);
		}
		iterationsSuccessiveAvg.put(iteration, successiveItAvg);

		return successiveItAvg;

		// avgwaitTime = (1-phi)V_prev_iter + phi*V_iter where V are wait time averages
		// of past or current iterations
		// v_iter = calculateZonalAverageWaitTimes(waitTimes, zones);

		// for each zone in dict v_iter and for each zone in dict v_prev_iter
		// if they are same zone:
		// do an array sum:
		// for element in the arrays combine based on formula
		// update new array
		// add new array to zone in a new dict

		// update v_prev_iter
		// v_prev_iter = avgwaittime

	}
}
