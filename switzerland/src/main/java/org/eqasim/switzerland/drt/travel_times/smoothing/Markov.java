package org.eqasim.switzerland.drt.travel_times.smoothing;

import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.travel_times.DataStats;
import org.eqasim.switzerland.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;
import java.util.Map;

public class Markov {

    //wait time
    public static double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        int nIteration = iterationZonalAndTimeBinWaitingTime.size() - 1;
        if (iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone) == null ||
                timeBin >= iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone).length ||
                Double.isNaN(iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone)[timeBin].getStat(feedback))) {
            return Double.NaN;
        }
        return iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone)[timeBin].getStat(feedback);
    }

    //delay factor
    public static double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        int nIteration = iterationDistanceAndTimeBinDelayFactor.size() - 1;
        if (iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin) == null ||
                timeBin >= iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin).length ||
                Double.isNaN(iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback))) {
            return Double.NaN;
        }
        return iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback);
    }
}
