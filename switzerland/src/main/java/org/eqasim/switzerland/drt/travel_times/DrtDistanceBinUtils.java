package org.eqasim.switzerland.drt.travel_times;

public class DrtDistanceBinUtils {
    private final int distanceBinSize_m;
    private final int lastBinStartDistance_m;
    private final int nDistanceBins;
    private final int lastBinStart_m;

    public DrtDistanceBinUtils(int distanceBinSize_m) {
        this(distanceBinSize_m, 10000);
    }

    // We do the following bins:
    // [0, distanceBinSize_m), [distanceBinSize_m, 2 * distanceBinSize_m), ...,
    // [(nDistanceBins-2) * distanceBinSize_m, (nDistanceBins-1) * distanceBinSize_m), [(nDistanceBins-1) * distanceBinSize_m, +inf)
    // Where the number of bins is determined by a maximum distance over which we want just to have one bin
    // In case the distanceBinSize_m is -1 we only have one bin for all the distances
    public DrtDistanceBinUtils(int distanceBinSize_m, int lastBinStartDistance_m) {
        this.distanceBinSize_m = distanceBinSize_m;
        this.lastBinStartDistance_m = lastBinStartDistance_m;
        if (this.distanceBinSize_m == -1) {
            this.nDistanceBins = 1;
            this.lastBinStart_m = 0;
        } else {
            this.nDistanceBins = (int) Math.ceil((double) this.lastBinStartDistance_m / this.distanceBinSize_m) + 1;
            this.lastBinStart_m = (nDistanceBins - 1) * distanceBinSize_m;
        }
    }

    public int getBinIndex(double distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        if (distance >= this.lastBinStart_m) {
            return this.nDistanceBins - 1;
        }
        return (int) Math.floor(distance / this.distanceBinSize_m);
    }

    public int getBinCount() {
        return this.nDistanceBins;
    }
}
