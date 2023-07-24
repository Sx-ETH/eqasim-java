package org.eqasim.core.components.drt.travel_times;

public class DrtTimeUtils {
    private final double timeBinSize_min;
    public final int nHours;

    public DrtTimeUtils(double timeBinSize_min, int nHours) {
        this.timeBinSize_min = timeBinSize_min;
        this.nHours = nHours;
    }

    public int getBinIndex(double time) {
        return (int) Math.floor(time / (this.timeBinSize_min * 60));
    }

    public int getBinCount() {
        return (int) Math.ceil((this.nHours * 60) / this.timeBinSize_min);
    }

}
