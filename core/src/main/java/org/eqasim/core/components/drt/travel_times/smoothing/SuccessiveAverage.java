package org.eqasim.core.components.drt.travel_times.smoothing;

import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.DataStats;
import org.eqasim.core.components.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;
import java.util.Map;

public class SuccessiveAverage implements Smoothing {
    private final double msaWeight;

    public SuccessiveAverage(double msaWeight) {
        this.msaWeight = msaWeight;
    }

    //wait time zonal
    @Override
    public double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                                   DrtModeChoiceConfigGroup.Feedback feedback) {
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double waitTime = Double.NaN;
        for (Map<String, DataStats[]> stringMap : iterationZonalAndTimeBinWaitingTime) {
            if (!(stringMap.get(zone) == null ||
                    timeBin >= stringMap.get(zone).length ||
                    Double.isNaN(stringMap.get(zone)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(waitTime)) {
                    waitTime = stringMap.get(zone)[timeBin].getStat(feedback);
                } else {
                    waitTime = waitTime * (1 - this.msaWeight) + stringMap.get(zone)[timeBin].getStat(feedback) * this.msaWeight;
                }
            }
        }
        return waitTime;
    }

    //wait time dynamic
    @Override
    public double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                                     DrtModeChoiceConfigGroup.Feedback feedback) {
        //dynamicWaitTime is the value of the last iteration
        ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = drtZoneMetricData.getDataZonalAndTimeBinWaitingTimes();
        double waitTime = Double.NaN;
        for (int i = 0; i < iterationZonalAndTimeBinWaitingTime.size() - 1; i++) {
            Map<String, DataStats[]> stringMap = iterationZonalAndTimeBinWaitingTime.get(i);
            if (!(stringMap.get(zone) == null ||
                    timeBin >= stringMap.get(zone).length ||
                    Double.isNaN(stringMap.get(zone)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(waitTime)) {
                    waitTime = stringMap.get(zone)[timeBin].getStat(feedback);
                } else {
                    waitTime = waitTime * (1 - this.msaWeight) + stringMap.get(zone)[timeBin].getStat(feedback) * this.msaWeight;
                }
            }

        }
        waitTime = waitTime * (1 - this.msaWeight) + dynamicWaitTime * this.msaWeight;

        return waitTime;
    }

    //delay time
    @Override
    public double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin,
                                 DrtModeChoiceConfigGroup.Feedback feedback) {
        ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = drtZoneMetricData.getDataDistanceAndTimeBinDelayFactor();
        double delayFactor = Double.NaN;
        for (Map<Integer, DataStats[]> integerMap : iterationDistanceAndTimeBinDelayFactor) {
            if (!(integerMap.get(distanceBin) == null ||
                    timeBin >= integerMap.get(distanceBin).length ||
                    Double.isNaN(integerMap.get(distanceBin)[timeBin].getStat(feedback)))) {
                if (Double.isNaN(delayFactor)) {
                    delayFactor = integerMap.get(distanceBin)[timeBin].getStat(feedback);
                } else {
                    delayFactor = delayFactor * (1 - this.msaWeight) + integerMap.get(distanceBin)[timeBin].getStat(feedback) * this.msaWeight;
                }
            }
        }
        return delayFactor;

    }

}
