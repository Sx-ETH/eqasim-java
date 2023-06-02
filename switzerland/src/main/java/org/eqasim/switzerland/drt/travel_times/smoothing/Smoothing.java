package org.eqasim.switzerland.drt.travel_times.smoothing;

import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.travel_times.zonal.DrtFixedZoneMetrics;

public interface Smoothing {
    double getZonalWaitTime(DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                            DrtModeChoiceConfigGroup.Feedback feedback);

    double getDelayFactor(DrtFixedZoneMetrics drtZoneMetricData, int distanceBin, int timeBin,
                          DrtModeChoiceConfigGroup.Feedback feedback);

    double getDynamicWaitTime(Double dynamicWaitTime, DrtFixedZoneMetrics drtZoneMetricData, String zone, int timeBin,
                              DrtModeChoiceConfigGroup.Feedback feedback);
}
