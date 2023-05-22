package org.eqasim.switzerland.drt.travel_times.smoothing;

import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.travel_times.DataStats;
import org.eqasim.switzerland.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;
import java.util.Map;

public class MovingWindow {
    //wait time zonal
    public static double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                          int movingWindow) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double total = 0;
        int count = 0;
        int startIteration = Math.max(0, iterationZonalAndTimeBinWaitingTime.size() - movingWindow);
        for (int i = startIteration; i < iterationZonalAndTimeBinWaitingTime.size(); i++) {
            if (!(iterationZonalAndTimeBinWaitingTime.get(i).get(zone) == null ||
                    timeBin >= iterationZonalAndTimeBinWaitingTime.get(i).get(zone).length ||
                    Double.isNaN(iterationZonalAndTimeBinWaitingTime.get(i).get(zone)[timeBin].getStat(feedback)))) {
                total += iterationZonalAndTimeBinWaitingTime.get(i).get(zone)[timeBin].getStat(feedback);
                count++;
            }
        }
        return total / count;
    }
    //wait time dynamic
    public static double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                            int movingWindow) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double total = 0;
        int count = 0;
        int startIteration = Math.max(0, iterationZonalAndTimeBinWaitingTime.size() - movingWindow);
        for (int i = startIteration; i < iterationZonalAndTimeBinWaitingTime.size()-1; i++) {
            if (!(iterationZonalAndTimeBinWaitingTime.get(i).get(zone) == null ||
                    timeBin >= iterationZonalAndTimeBinWaitingTime.get(i).get(zone).length ||
                    Double.isNaN(iterationZonalAndTimeBinWaitingTime.get(i).get(zone)[timeBin].getStat(feedback)))) {
                total += iterationZonalAndTimeBinWaitingTime.get(i).get(zone)[timeBin].getStat(feedback);
                count++;
            }
        }
        total = total+ dynamicWaitTime;
        return total / (count + 1);
    }


    //delay time
    public static double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                        int movingWindow) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        double total = 0;
        int count = 0;
        int startIteration = Math.max(0, iterationDistanceAndTimeBinDelayFactor.size() - movingWindow);
        for (int i = startIteration; i < iterationDistanceAndTimeBinDelayFactor.size(); i++) {
            if (!(iterationDistanceAndTimeBinDelayFactor.get(i).get(distanceBin) == null ||
                    timeBin >= iterationDistanceAndTimeBinDelayFactor.get(i).get(distanceBin).length ||
                    Double.isNaN(iterationDistanceAndTimeBinDelayFactor.get(i).get(distanceBin)[timeBin].getStat(feedback)))) {
                total += iterationDistanceAndTimeBinDelayFactor.get(i).get(distanceBin)[timeBin].getStat(feedback);
                count++;
            }
        }
        return total / count;
    }

}
