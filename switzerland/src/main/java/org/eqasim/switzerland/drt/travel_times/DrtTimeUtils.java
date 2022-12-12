package org.eqasim.switzerland.drt.travel_times;

public class DrtTimeUtils {
	public static final double TIME_WINDOW_MIN = 60;

	public static int getTimeBin(double time) {
		return (int) Math.floor(time / (TIME_WINDOW_MIN * 60));
	}

	public static int getWaitingTimeBinCount() {
		// TODO: Get the number of hours from the config file?
		return (int) Math.ceil((24 * 60) / TIME_WINDOW_MIN);
	}

}
