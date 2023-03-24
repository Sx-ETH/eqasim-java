package org.eqasim.switzerland.drt.travel_times.zonal;


import org.eqasim.switzerland.drt.travel_times.DrtTimeUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTripData;
import org.eqasim.switzerland.drt.travel_times.TravelTimeData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DrtFixedZoneMetrics {
    private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
    private static final Map<Integer, TravelTimeData> iterationsSuccessiveAvg = new HashMap<>();

    private static Map<String, Set<DrtTripData>[]> assignDrtTripsToZonesAndTimeBin(Set<DrtTripData> drtTrips, FixedDrtZonalSystem zones, int timeBinSize_min) {
        DrtTimeUtils timeUtils = new DrtTimeUtils(timeBinSize_min);
        int nTimeBins = timeUtils.getBinCount();

        Map<String, Set<DrtTripData>[]> drtTripsByZoneAndTimeBin = new HashMap<>();
        for (DrtTripData drtTrip : drtTrips) {
            String zone = zones.getZoneForLinkId(drtTrip.startLinkId);
            int timeBin = timeUtils.getBinIndex(drtTrip.startTime);
            if (drtTripsByZoneAndTimeBin.containsKey(zone)) {
                drtTripsByZoneAndTimeBin.get(zone)[timeBin].add(drtTrip);
            } else {
                Set<DrtTripData>[] drtTripsByTimeBin = new Set[nTimeBins];
                for (int i = 0; i < nTimeBins; i++) {
                    drtTripsByTimeBin[i] = new HashSet<>();
                }
                drtTripsByTimeBin[timeBin].add(drtTrip);
                drtTripsByZoneAndTimeBin.put(zone, drtTripsByTimeBin);
            }
        }
        return drtTripsByZoneAndTimeBin;
    }

    private static double[] collectWaitTimes(Set<DrtTripData> drtTrips) {
        double[] l = new double[drtTrips.size()];

        int i = 0;
        for (DrtTripData drtTrip : drtTrips) {
            l[i] = drtTrip.waitTime;
            ++i;
        }
        return l;
    }

    private static double[] collectDelayFactors(Set<DrtTripData> drtTrips) {
        double[] l = new double[drtTrips.size()];
        int i = 0;
        for (DrtTripData drtTrip : drtTrips) {
            if (drtTrip.routerUnsharedTime > 0) {
                l[i] = drtTrip.totalTravelTime / drtTrip.routerUnsharedTime;
            } else {
                l[i] = drtTrip.totalTravelTime / drtTrip.estimatedUnsharedTime;
            }
            ++i;
        }
        return l;
    }

    private static double calculateAverageDelayFactorFromSums(Set<DrtTripData> drtTrips) {
        double sumTotalTravelTime = 0.0;
        double sumUnsharedTime = 0.0;
        boolean useRouterUnsharedTime = true; // may remove later when decided on whether to use router or estimate

        for (DrtTripData drtTrip : drtTrips) {
            sumTotalTravelTime += drtTrip.totalTravelTime;
            if (useRouterUnsharedTime) {
                sumUnsharedTime += drtTrip.routerUnsharedTime;
            } else {
                sumUnsharedTime += drtTrip.estimatedUnsharedTime;
            }
        }

        // what happens if no drt trip occurred in an iteration
        // division by 0 gives nan/inf and is returned

        // compute delay factor
        return sumTotalTravelTime / sumUnsharedTime;

    }

    public static Map<String, TravelTimeData[]> calculateZonalAndTimeBinMetrics(Set<DrtTripData> drtTrips, String delayFactorMethod, FixedDrtZonalSystem zones, int timeBinSize_min) {
        Map<String, TravelTimeData[]> zonalAndTimeBinMetrics = new HashMap<>();
        Map<String, Set<DrtTripData>[]> assignedDrtTrips = assignDrtTripsToZonesAndTimeBin(drtTrips, zones, timeBinSize_min);

        for (String zone : assignedDrtTrips.keySet()) {
            Set<DrtTripData>[] drtTripsByTimeBin = assignedDrtTrips.get(zone);
            TravelTimeData[] zonalAndTimeBinMetricsByZone = new TravelTimeData[drtTripsByTimeBin.length];
            for (int i = 0; i < drtTripsByTimeBin.length; i++) {
                Set<DrtTripData> drtTripsByTimeBin_i = drtTripsByTimeBin[i];
                double[] waitTimes = collectWaitTimes(drtTripsByTimeBin_i);
                TravelTimeData travelTimeData;
                if (delayFactorMethod.equals("divisionOfSums")) {
                    double avgDF = calculateAverageDelayFactorFromSums(drtTrips);
                    travelTimeData = new TravelTimeData(waitTimes, avgDF);

                } else {
                    double[] delayFactors = collectDelayFactors(drtTripsByTimeBin_i);
                    travelTimeData = new TravelTimeData(waitTimes, delayFactors);
                }
                zonalAndTimeBinMetricsByZone[i] = travelTimeData;
            }
            zonalAndTimeBinMetrics.put(zone, zonalAndTimeBinMetricsByZone);
        }
        return zonalAndTimeBinMetrics;
    }

}
