package org.eqasim.switzerland.drt.travel_times;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.io.IOUtils;


public class DrtGlobalMetrics {
	private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
	private static final Map<Integer, TravelTimeData> iterationsSuccessiveAvg = new HashMap<>();

	@Inject
	private static ExperiencedPlansService experiencedPlansService;


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

	private static int getTripIndexFromDrtLeg(Id<Person> personId, double startTime){
		Plan personPlan = experiencedPlansService.getExperiencedPlans().get(personId);
		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(personPlan);
		for (int i = 0; i < trips.size(); i++) {
			TripStructureUtils.Trip trip = trips.get(i);
			int tripIndex = i; // We start at 0 to be able to match it with the predictions
			for (Leg leg : trip.getLegsOnly()) {
				if (leg.getMode().equals("drt") && leg.getDepartureTime().seconds() == startTime) {
					return tripIndex;
				}
			}
		}
		throw new RuntimeException("Could not find tripId for drt leg of person " + personId + " at time " + startTime);

	}

	public static void writeDrtTripsStats(Set<DrtTripData> drtTrips, int iterationNumber, Config config)
			throws IOException {
		String delimiter = ",";
		String filename = "drt_drtTripsStats.csv";
		String outputDir = config.controler().getOutputDirectory() + "/ITERS/it." + iterationNumber + "/"
				+ iterationNumber + "." + filename;
		BufferedWriter bw = IOUtils.getBufferedWriter(outputDir);
		bw.write("personId" + delimiter + "tripIndex" + delimiter + "startTime" + delimiter + "arrivalTime" + delimiter + "totalTravelTime" + delimiter + "routerUnsharedTime"
				+ delimiter + "estimatedUnsharedTime" + delimiter + "delayFactor" + delimiter + "waitTime" + delimiter
				+ "startX" + delimiter + "startY" + delimiter + "endX" + delimiter + "endY");

		for (DrtTripData drtTrip : drtTrips) {
			// print out to csv, the estimatedUnsharedTime from requestSubmissionEvent
			// and compare with this computed one from the route

			// We first find the id of the trip to be able to match it with the predictions
			int tripIndex = getTripIndexFromDrtLeg(drtTrip.personId, drtTrip.startTime);

			bw.newLine();

			double delayFactor;
			if (drtTrip.routerUnsharedTime > 0) {
				delayFactor = drtTrip.totalTravelTime / drtTrip.routerUnsharedTime;
			} else {
				delayFactor = drtTrip.totalTravelTime / drtTrip.estimatedUnsharedTime;
			}

			bw.write(drtTrip.personId + delimiter + tripIndex + delimiter + drtTrip.startTime + delimiter + drtTrip.arrivalTime + delimiter + drtTrip.totalTravelTime + delimiter
					+ drtTrip.routerUnsharedTime + delimiter + drtTrip.estimatedUnsharedTime + delimiter + delayFactor
					+ delimiter + drtTrip.waitTime + delimiter + drtTrip.startCoord.getX() + delimiter
					+ drtTrip.startCoord.getY() + delimiter + drtTrip.endCoord.getX() + delimiter
					+ drtTrip.endCoord.getY());

		}

		bw.flush();
		bw.close();
	}

}
