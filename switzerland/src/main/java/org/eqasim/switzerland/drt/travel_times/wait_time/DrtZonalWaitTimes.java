package org.eqasim.switzerland.drt.travel_times.wait_time;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class DrtZonalWaitTimes implements IterationEndsListener {
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

    }
/*
	private final DrtTimeTracker trackedWaitTimes;
	private Map<String, double[]> avgWaitTimes;
	SquareGridDrtZonalSystem zones;

	@Inject
	public DrtZonalWaitTimes(DrtTimeTracker trackedWaitTimes, SquareGridDrtZonalSystem zones, Config config) {

		this.trackedWaitTimes = trackedWaitTimes;
		this.avgWaitTimes = new HashMap<>();
		this.zones = zones;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// generate wait times for use.
		Map<String, double[]> avgWaitTimesZonal = WaitTimeZonalMetrics
				.calculateZonalAverageWaitTimes(trackedWaitTimes.getDrtTrips(), zones);
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalAvg.csv");
		write(fileName, avgWaitTimesZonal);

		Map<String, double[]> movingAvgWaitTimesZonal = WaitTimeZonalMetrics
				.calculateMovingZonalAverageWaitTimes(trackedWaitTimes.getDrtTrips(), zones, event.getIteration(), 2);
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalMovingAvg.csv");

		write(fileName, movingAvgWaitTimesZonal);

		Map<String, double[]> successiveAvgWaitTimesZonal = WaitTimeZonalMetrics.calculateMethodOfSuccessiveAverageWaitTimes(
				trackedWaitTimes.getDrtTrips(), zones, event.getIteration(), 0.5);

		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"DrtWaitTimesZonalSuccessiveAvg.csv");

		write(fileName, successiveAvgWaitTimesZonal);

		String method = "global";
		switch (method) {
		case "global":
			this.avgWaitTimes = avgWaitTimesZonal;
			break;
		case "moving":
			this.avgWaitTimes = movingAvgWaitTimesZonal;
			break;
		case "successive":
			this.avgWaitTimes = successiveAvgWaitTimesZonal;
			break;
		default:
			throw new IllegalArgumentException(
					"Method for computing avergae has to be one of [global, moving, successive]");

		}
	}

	private void write(String fileName, Map<String, double[]> valuesToWrite) {
		String delimiter = ";";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);

		String firstLine = "zone;";
		int timeBins = DrtTimeUtils.getWaitingTimeBinCount();
		int[] array = IntStream.range(0, timeBins).toArray();
		firstLine = firstLine.concat(StringUtils.join(ArrayUtils.toObject(array), delimiter));

		try {
			writer.append(firstLine);

			valuesToWrite.forEach((k, v) -> {
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

 */
}