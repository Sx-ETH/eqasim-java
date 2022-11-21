package org.eqasim.switzerland.drt.wait_time;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

public class DrtWaitTimeGlobal implements IterationEndsListener {
	
	private final WaitTimeTracker trackedWaitTimes;
	private double avgWaitTime;
	
	@Inject
	public DrtWaitTimeGlobal(WaitTimeTracker trackedWaitTimes) {
		this.trackedWaitTimes = trackedWaitTimes;
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.avgWaitTime = WaitTimeGlobalMetrics.calculateAverageWaitTime(trackedWaitTimes.getDrtTrips());
		String fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "DrtWaitTimeGlobalAvg.csv");
		write(fileName);
		
		this.avgWaitTime = WaitTimeGlobalMetrics.calculateMovingAverageWaitTime(trackedWaitTimes.getDrtTrips(), event.getIteration(), 2);
		fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "DrtWaitTimeGlobalMovingAvg.csv");
		write(fileName);
		
		this.avgWaitTime = WaitTimeGlobalMetrics.calculateMethodOfSuccessiveAverageWaitTime(trackedWaitTimes.getDrtTrips(), event.getIteration(), 0.5);
		fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "DrtWaitTimeGlobalSuccessiveAvg.csv");
		write(fileName);
		
		

	}
	
	private void write(String fileName) {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		
		try {
			writer.append("zone;avg\n");
			writer.append("Global_avg;");
			writer.append(String.valueOf(this.avgWaitTime));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public double getAvgWaitTime() {
		return avgWaitTime;
	}

}
