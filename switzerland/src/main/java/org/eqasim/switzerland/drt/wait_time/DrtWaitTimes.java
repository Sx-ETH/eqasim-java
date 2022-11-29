package org.eqasim.switzerland.drt.wait_time;

import com.google.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class DrtWaitTimes implements IterationEndsListener {

	private final WaitTimeTracker trackedWaitTimes;
	private Map<String, double[]> avgWaitTimes;
	WayneCountyDrtZonalSystem zones;

	@Inject
	public DrtWaitTimes(WaitTimeTracker trackedWaitTimes, WayneCountyDrtZonalSystem zones, Config config) {

		this.trackedWaitTimes = trackedWaitTimes;
		this.avgWaitTimes = new HashMap<>();
		this.zones = zones;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// generate wait times for use.
		this.avgWaitTimes = WaitTimeMetrics.calculateZonalAverageWaitTimes(trackedWaitTimes.getDrtTrips(), zones);
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalAvg.csv");
		write(fileName);

		// toDo if...then configuration for what method to use - define moving window
		this.avgWaitTimes = WaitTimeMetrics.calculateMovingZonalAverageWaitTimes(trackedWaitTimes.getDrtTrips(), zones,
				event.getIteration(), 2);
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalMovingAvg.csv");

		write(fileName);

		this.avgWaitTimes = WaitTimeMetrics.calculateMethodOfSuccessiveAverageWaitTimes(trackedWaitTimes.getDrtTrips(), zones, event.getIteration(), 0.5);
		
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalSuccessiveAvg.csv");

		write(fileName);
	}

	private void write(String fileName) {
		String delimiter = ";";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);

		String firstLine = "zone;";
		int timeBins = 30; // toDo: Get from config or avgWaitTime
		int[] array = IntStream.range(0, timeBins).toArray();
		firstLine = firstLine.concat(StringUtils.join(ArrayUtils.toObject(array), delimiter));

		try {
			writer.append(firstLine);

			this.avgWaitTimes.forEach((k, v) -> {
				try {
					writer.append("\n").append(k).append(delimiter)
							.append(StringUtils.join(ArrayUtils.toObject(v), delimiter));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Map<String, double[]> getAvgWaitTimes() {

		return avgWaitTimes;
	}
}