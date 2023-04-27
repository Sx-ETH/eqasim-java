package org.eqasim.switzerland.drt.travel_times.zonal;


import org.eqasim.switzerland.drt.travel_times.DataStats;
import org.eqasim.switzerland.drt.travel_times.DrtDistanceBinUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTimeUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTripData;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DrtFixedZoneMetrics {
    //private static final Map<Integer, Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
    //private static final Map<Integer, TravelTimeData> iterationsSuccessiveAvg = new HashMap<>();

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

    private static Map<Integer, Set<DrtTripData>[]> assignDrtTripsToDistanceAndTimeBin(Set<DrtTripData> drtTrips, int distanceBinSize_m, int timeBinSize_min) {
        DrtTimeUtils timeUtils = new DrtTimeUtils(timeBinSize_min);
        int nTimeBins = timeUtils.getBinCount();

        DrtDistanceBinUtils distanceUtils = new DrtDistanceBinUtils(distanceBinSize_m);
        int nDistanceBins = distanceUtils.getBinCount();

        // The first index is the distance bin, the second index is the time bin (to be consistent with the other functions)
        Map<Integer, Set<DrtTripData>[]> drtTripsByDistanceAndTimeBin = new HashMap<>();

        // We create empty sets for all the bins
        for (int i = 0; i < nDistanceBins; i++) {
            Set<DrtTripData>[] drtTripsByTimeBin = new Set[nTimeBins];
            for (int j = 0; j < nTimeBins; j++) {
                drtTripsByTimeBin[j] = new HashSet<>();
            }
            drtTripsByDistanceAndTimeBin.put(i, drtTripsByTimeBin);
        }
        for (DrtTripData drtTrip : drtTrips) {
            int timeBin = timeUtils.getBinIndex(drtTrip.startTime);
            double distance_m = CoordUtils.calcEuclideanDistance(drtTrip.startCoord, drtTrip.endCoord);
            int distanceBin = distanceUtils.getBinIndex(distance_m);
            drtTripsByDistanceAndTimeBin.get(distanceBin)[timeBin].add(drtTrip);
        }
        return drtTripsByDistanceAndTimeBin;
    }

    public static double[] collectWaitTimes(Set<DrtTripData> drtTrips) {
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

    public static Map<String, DataStats[]> calculateZonalAndTimeBinWaitingTime(Set<DrtTripData> drtTripData, FixedDrtZonalSystem zones, int timeBinSize_min) {
        Map<String, DataStats[]> zonalAndTimeBinWaitTime = new HashMap<>();
        Map<String, Set<DrtTripData>[]> assignedDrtTrips = assignDrtTripsToZonesAndTimeBin(drtTripData, zones, timeBinSize_min);

        for (String zone : assignedDrtTrips.keySet()) {
            Set<DrtTripData>[] drtTripsByTimeBin = assignedDrtTrips.get(zone);
            DataStats[] zoneWaitingTimeByTimeBins = new DataStats[drtTripsByTimeBin.length];
            for (int i = 0; i < drtTripsByTimeBin.length; i++) {
                Set<DrtTripData> drtTripsByTimeBinByZone = drtTripsByTimeBin[i];
                double[] waitTimes = collectWaitTimes(drtTripsByTimeBinByZone);
                zoneWaitingTimeByTimeBins[i] = new DataStats(waitTimes);
            }
            zonalAndTimeBinWaitTime.put(zone, zoneWaitingTimeByTimeBins);
        }
        return zonalAndTimeBinWaitTime;
    }

    public static Map<Integer, DataStats[]> calculateDistanceAndTimeBinDelayFactor(Set<DrtTripData> drtTripData, int distanceBinSize_m, int timeBinSize_min) {
        Map<Integer, DataStats[]> distanceAndTimeBinDelayFactor = new HashMap<>();
        Map<Integer, Set<DrtTripData>[]> assignedDrtTrips = assignDrtTripsToDistanceAndTimeBin(drtTripData, distanceBinSize_m, timeBinSize_min);

        for (Integer distanceBin : assignedDrtTrips.keySet()) {
            Set<DrtTripData>[] drtTripsByTimeBin = assignedDrtTrips.get(distanceBin);
            DataStats[] distanceBinDelayFactorByTimeBins = new DataStats[drtTripsByTimeBin.length];
            for (int i = 0; i < drtTripsByTimeBin.length; i++) {
                Set<DrtTripData> drtTripsByTimeBinByDistanceBin = drtTripsByTimeBin[i];
                double[] delayFactors = collectDelayFactors(drtTripsByTimeBinByDistanceBin);
                distanceBinDelayFactorByTimeBins[i] = new DataStats(delayFactors);
            }
            distanceAndTimeBinDelayFactor.put(distanceBin, distanceBinDelayFactorByTimeBins);
        }
        return distanceAndTimeBinDelayFactor;
    }

    public static DataStats calculateGlobalWaitingTime(Set<DrtTripData> drtTripData) {
        double[] waitTimes = collectWaitTimes(drtTripData);
        return new DataStats(waitTimes);
    }

    public static DataStats calculateGlobalDelayFactor(Set<DrtTripData> drtTripData) {
        double[] delayFactors = collectDelayFactors(drtTripData);
        return new DataStats(delayFactors);
    }
}
