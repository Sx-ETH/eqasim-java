package org.eqasim.switzerland.drt.travel_times;

import java.io.IOException;
import java.util.Set;

import org.eqasim.switzerland.drt.SimulationParameter;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

public class TravelTimeUpdates implements IterationEndsListener {
	private final SimulationParameter simulationParams;
	private WayneCountyDrtZonalSystem zones;
	private final DrtTimeTracker trackedTimes;
	private TravelTimeData travelTimeData;
	private Config config;

	@Inject
	public TravelTimeUpdates(SimulationParameter simulationParams, WayneCountyDrtZonalSystem zones,
			DrtTimeTracker trackedTimes, Config config) {
		this.simulationParams = simulationParams;
		this.zones = zones;
		this.trackedTimes = trackedTimes;
		this.config = config;
	}

	public void test() {

		boolean useAverageWaitTime = true; // toDo cmd config should say to use avg wait time or not

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
		 */}

	public double getTravelTime(DrtRoute route) {
		return 0.0;
	}

	public double getWaitTime(DrtRoute route) {
		return 0.0;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO: Same method for both calculations, should we allow it to be different?
		String method = this.simulationParams.getDelayCalcMethod();
		double weight = this.simulationParams.getMsaWeight();
		int movingWindow = this.simulationParams.getMovingWindow();
		Set<DrtTripData> drtTrips = this.trackedTimes.getDrtTrips();

		// TODO: Only compute the needed one -> this is to test that it works correctly
		TravelTimeData globalAvg = DrtGlobalMetrics.calculateGlobalMetrics(drtTrips);
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"drt_travelTimeData_global.csv");
		globalAvg.write(fileName);

		TravelTimeData movingAvg = DrtGlobalMetrics.calculateGlobalMovingMetrics(drtTrips, event.getIteration(),
				movingWindow);
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"drt_travelTimeData_moving.csv");
		movingAvg.write(fileName);

		TravelTimeData successiveAvg = DrtGlobalMetrics.calculateMethodOfSuccessiveAverageWaitTime(drtTrips,
				event.getIteration(), weight);
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
			throw new IllegalArgumentException("Method not implemented yet!");
		}

		try {
			DrtGlobalMetrics.writeDrtTripsStats(drtTrips, event.getIteration(), this.config);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
