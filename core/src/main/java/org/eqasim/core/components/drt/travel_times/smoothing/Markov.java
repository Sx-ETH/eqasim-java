package org.eqasim.core.components.drt.travel_times.smoothing;


import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.DataStats;
import org.eqasim.core.components.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;
import java.util.Map;

public class Markov implements Smoothing {

    //wait time
    @Override
    public double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                                   DrtModeChoiceConfigGroup.Feedback feedback) {
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
    @Override
    public double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin,
                                 DrtModeChoiceConfigGroup.Feedback feedback) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        int nIteration = iterationDistanceAndTimeBinDelayFactor.size() - 1;
        if (iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin) == null ||
                timeBin >= iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin).length ||
                Double.isNaN(iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback))) {
            return Double.NaN;
        }
        return iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback);
    }

    //dynamic wait time
    @Override
    public double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone,
                                     int timeBin, DrtModeChoiceConfigGroup.Feedback feedback) {
        return dynamicWaitTime;
    }

    public static DataStats getZonalWaitTimeStats(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        int nIteration = iterationZonalAndTimeBinWaitingTime.size() - 1;
        if (iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone) == null ||
                timeBin >= iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone).length) {
            return new DataStats();
        }
        return iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone)[timeBin];
    }

    public static DataStats getDelayFactorStats(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        int nIteration = iterationDistanceAndTimeBinDelayFactor.size() - 1;
        if (iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin) == null ||
                timeBin >= iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin).length) {
            return new DataStats();
        }
        return iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin];
    }

    public static DataStats getDynamicWaitTimeStats(DataStats dynamicWaitTimeStats, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin) {
        return dynamicWaitTimeStats;
    }
}
