package org.eqasim.switzerland.drt.travel_times.wait_time;

import java.io.BufferedWriter;
import java.io.IOException;

import org.eqasim.switzerland.drt.travel_times.DrtTimeTracker;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

public class DrtWaitTimeGlobal implements IterationEndsListener {

	private final DrtTimeTracker trackedWaitTimes;
	private double avgWaitTime;

	@Inject
	public DrtWaitTimeGlobal(DrtTimeTracker trackedWaitTimes) {
		this.trackedWaitTimes = trackedWaitTimes;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		double avgWaitTimeGlobal = WaitTimeGlobalMetrics.calculateAverageWaitTime(trackedWaitTimes.getDrtTrips());
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimeGlobalAvg.csv");
		write(fileName, avgWaitTimeGlobal);

		double movingAvgWaitTimeGlobal = WaitTimeGlobalMetrics
				.calculateMovingAverageWaitTime(trackedWaitTimes.getDrtTrips(), event.getIteration(), 2);
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimeGlobalMovingAvg.csv");
		write(fileName, movingAvgWaitTimeGlobal);

		double successiveAvgWaitTimeGlobal = WaitTimeGlobalMetrics
				.calculateMethodOfSuccessiveAverageWaitTime(trackedWaitTimes.getDrtTrips(), event.getIteration(), 0.5);
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimeGlobalSuccessiveAvg.csv");
		write(fileName, successiveAvgWaitTimeGlobal);

		String method = "global";
		switch (method) {
		case "global":
			this.avgWaitTime = avgWaitTimeGlobal;
			break;
		case "moving":
			this.avgWaitTime = movingAvgWaitTimeGlobal;
			break;
		case "successive":
			this.avgWaitTime = successiveAvgWaitTimeGlobal;
			break;
		default:
			throw new IllegalArgumentException(
					"Method for computing avergae has to be one of [global, moving, successive]");

		}

	}

	private void write(String fileName, double value) {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);

		try {
			writer.append("zone;avg\n");
			writer.append("Global_avg;");
			writer.append(String.valueOf(value));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public double getAvgWaitTime() {
		if (!Double.isNaN(avgWaitTime)) {
			return avgWaitTime;
		}
		else {
			return 10.0 * 60;
		}
	}

}
