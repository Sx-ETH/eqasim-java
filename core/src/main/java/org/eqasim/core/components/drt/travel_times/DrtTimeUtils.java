package org.eqasim.core.components.drt.travel_times;

public class DrtTimeUtils {
    private final double timeBinSize_min;

    public DrtTimeUtils(double timeBinSize_min) {
        this.timeBinSize_min = timeBinSize_min;
    }

    public int getBinIndex(double time) {
        return (int) Math.floor(time / (this.timeBinSize_min * 60));
    }

    public int getBinCount() {
        // TODO: Get the number of hours from the config file?
        return (int) Math.ceil((24 * 60) / this.timeBinSize_min);
    }

}