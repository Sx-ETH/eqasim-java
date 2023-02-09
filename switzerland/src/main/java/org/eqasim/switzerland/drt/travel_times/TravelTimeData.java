package org.eqasim.switzerland.drt.travel_times;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.utils.io.IOUtils;

public class TravelTimeData {
	// wait time data
	public StatData waitTimeStat; // = new StatData();
	public StatData delayFactorStat; // = new StatData();
	public int tripNum;

	public TravelTimeData() {
		this.waitTimeStat = new StatData();
		this.delayFactorStat = new StatData();
	}

	public TravelTimeData(double[] waitTimes, double[] delayFactors) {
		this.waitTimeStat = new StatData(waitTimes);
		this.delayFactorStat = new StatData(delayFactors);
		if (waitTimes.length != delayFactors.length) {
			throw new RuntimeException("Sizes of delayFactors and waitTimes not matching");
		}
		this.tripNum = waitTimes.length;
	}

	// delay time data
	class StatData {
		public double avg;
		public double median;
		public double min;
		public double p_5;
		public double p_25;
		public double p_75;
		public double p_95;
		public double max;

		public StatData() {
		}

		public StatData(double[] stats) {
			DescriptiveStatistics descStats = new DescriptiveStatistics(stats);
			this.avg = descStats.getMean();
			this.median = descStats.getPercentile(50);
			this.min = descStats.getMin();
			this.p_5 = descStats.getPercentile(5);
			this.p_25 = descStats.getPercentile(25);
			this.p_75 = descStats.getPercentile(75);
			this.p_95 = descStats.getPercentile(95);
			this.max = descStats.getMax();

		}
	}

	private static double combineIterations(double previousIt, double currentIt, double weight) {
		// In the case the previous avg is Nan then we only use the current average
		if (Double.isNaN(previousIt)) {
			return currentIt;
		}
		// In the case there are no trips in this iteration then we keep the value
		// estimated in the previous iteration (so we'll never have nans after one
		// correct value)
		if (Double.isNaN(currentIt)) {
			return previousIt;
		}
		return (1 - weight) * previousIt + weight * currentIt;
	}

	public static TravelTimeData combineIterationsSuccessive(TravelTimeData previousIt, TravelTimeData currentIt,
			double weight) {
		TravelTimeData combinedData = new TravelTimeData();

		combinedData.waitTimeStat.avg = combineIterations(previousIt.waitTimeStat.avg, currentIt.waitTimeStat.avg,
				weight);
		combinedData.waitTimeStat.median = combineIterations(previousIt.waitTimeStat.median,
				currentIt.waitTimeStat.median, weight);
		combinedData.waitTimeStat.min = combineIterations(previousIt.waitTimeStat.min, currentIt.waitTimeStat.min,
				weight);
		combinedData.waitTimeStat.p_5 = combineIterations(previousIt.waitTimeStat.p_5, currentIt.waitTimeStat.p_5,
				weight);
		combinedData.waitTimeStat.p_25 = combineIterations(previousIt.waitTimeStat.p_25, currentIt.waitTimeStat.p_25,
				weight);
		combinedData.waitTimeStat.p_75 = combineIterations(previousIt.waitTimeStat.p_75, currentIt.waitTimeStat.p_75,
				weight);
		combinedData.waitTimeStat.p_95 = combineIterations(previousIt.waitTimeStat.p_95, currentIt.waitTimeStat.p_95,
				weight);
		combinedData.waitTimeStat.max = combineIterations(previousIt.waitTimeStat.max, currentIt.waitTimeStat.max,
				weight);

		combinedData.delayFactorStat.avg = combineIterations(previousIt.delayFactorStat.avg,
				currentIt.delayFactorStat.avg, weight);
		combinedData.delayFactorStat.median = combineIterations(previousIt.delayFactorStat.median,
				currentIt.delayFactorStat.median, weight);
		combinedData.delayFactorStat.min = combineIterations(previousIt.delayFactorStat.min,
				currentIt.delayFactorStat.min, weight);
		combinedData.delayFactorStat.p_5 = combineIterations(previousIt.delayFactorStat.p_5,
				currentIt.delayFactorStat.p_5, weight);
		combinedData.delayFactorStat.p_25 = combineIterations(previousIt.delayFactorStat.p_25,
				currentIt.delayFactorStat.p_25, weight);
		combinedData.delayFactorStat.p_75 = combineIterations(previousIt.delayFactorStat.p_75,
				currentIt.delayFactorStat.p_75, weight);
		combinedData.delayFactorStat.p_95 = combineIterations(previousIt.delayFactorStat.p_95,
				currentIt.delayFactorStat.p_95, weight);
		combinedData.delayFactorStat.max = combineIterations(previousIt.delayFactorStat.max,
				currentIt.delayFactorStat.max, weight);

		return combinedData;
	}

	public void write(String fileName) {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.append("stat;avg;median;min;p_5;p_25;p_75;p_95;max\n");
			writer.append("waitTime;");
			writer.append(String.valueOf(this.waitTimeStat.avg));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.median));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.min));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.p_5));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.p_25));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.p_75));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.p_95));
			writer.append(";");
			writer.append(String.valueOf(this.waitTimeStat.max));
			writer.append("\n");

			writer.append("delayFactor;");
			writer.append(String.valueOf(this.delayFactorStat.avg));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.median));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.min));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.p_5));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.p_25));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.p_75));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.p_95));
			writer.append(";");
			writer.append(String.valueOf(this.delayFactorStat.max));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
