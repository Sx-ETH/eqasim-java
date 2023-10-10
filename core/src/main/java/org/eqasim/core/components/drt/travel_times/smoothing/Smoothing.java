package org.eqasim.core.components.drt.travel_times.smoothing;


import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.DataStats;
import org.eqasim.core.components.drt.travel_times.zonal.DrtFixedZoneMetrics;

import java.util.ArrayList;

public interface Smoothing {
    double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                            DrtModeChoiceConfigGroup.Feedback feedback);

    double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin,
                          DrtModeChoiceConfigGroup.Feedback feedback);

    double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                              DrtModeChoiceConfigGroup.Feedback feedback);

    double getGlobalData(ArrayList<DataStats> iterationGlobalData,
                         DrtModeChoiceConfigGroup.Feedback feedback);

}
