package org.eqasim.switzerland.drt.travel_times.smoothing;

import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.travel_times.DataStats;
import org.eqasim.switzerland.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;
import java.util.Map;

public class SuccessiveAverage {

    //wait time zonal
    public static double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                          double msaWeight) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double waitTime = Double.NaN;
        for (Map<String, DataStats[]> stringMap : iterationZonalAndTimeBinWaitingTime) {
            if (!(stringMap.get(zone) == null ||
                    timeBin >= stringMap.get(zone).length ||
                    Double.isNaN(stringMap.get(zone)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(waitTime)) {
                    waitTime = stringMap.get(zone)[timeBin].getStat(feedback);
                } else {
                    waitTime = waitTime * (1 - msaWeight) + stringMap.get(zone)[timeBin].getStat(feedback) * msaWeight;
                }
            }
        }
        return waitTime;
    }

    public static double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                            double msaWeight) {
        //dynamicWaitTime is the value of the last iteration
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double waitTime = Double.NaN;
        for(int i= 0; i<iterationZonalAndTimeBinWaitingTime.size()-1; i++){
            Map<String, DataStats[]> stringMap = iterationZonalAndTimeBinWaitingTime.get(i);
            if (!(stringMap.get(zone) == null ||
                    timeBin >= stringMap.get(zone).length ||
                    Double.isNaN(stringMap.get(zone)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(waitTime)) {
                    waitTime = stringMap.get(zone)[timeBin].getStat(feedback);
                } else {
                    waitTime = waitTime * (1 - msaWeight) + stringMap.get(zone)[timeBin].getStat(feedback) * msaWeight;
                }
            }

        }
        waitTime = waitTime*(1 - msaWeight) + dynamicWaitTime*msaWeight;

        return waitTime;
    }

    //delay time
    public static double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                        double msaWeight) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        double delayFactor = Double.NaN;
        for (Map<Integer, DataStats[]> integerMap : iterationDistanceAndTimeBinDelayFactor) {
            if (!(integerMap.get(distanceBin) == null ||
                    timeBin >= integerMap.get(distanceBin).length ||
                    Double.isNaN(integerMap.get(distanceBin)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(delayFactor)) {
                    delayFactor = integerMap.get(distanceBin)[timeBin].getStat(feedback);
                } else {
                    delayFactor = delayFactor * (1 - msaWeight) + integerMap.get(distanceBin)[timeBin].getStat(feedback) * msaWeight;
                }
            }
        }
        return delayFactor;

    }

    public double getDynamicWaitTime(){
        return Double.NaN;
    }
}
