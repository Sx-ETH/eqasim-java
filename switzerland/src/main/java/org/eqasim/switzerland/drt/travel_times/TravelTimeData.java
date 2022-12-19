package org.eqasim.switzerland.drt.travel_times;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class TravelTimeData {
	// wait time data
	public StatData waitTimeStat = new StatData();
	public StatData delayFactorStat = new StatData();
	public int tripNum;

	/*
	 * public TravelTimeData(){ this.waitTimeStat = new StatData();
	 * this.delayFactorStat = new StatData(); }
	 */

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
