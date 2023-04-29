package org.eqasim.switzerland.drt.travel_times.zonal;


import org.apache.log4j.Logger;
import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.travel_times.DataStats;
import org.eqasim.switzerland.drt.travel_times.DrtDistanceBinUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTimeUtils;
import org.eqasim.switzerland.drt.travel_times.DrtTripData;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class DrtFixedZoneMetrics {
    private final ArrayList<Map<String, DataStats[]>> iterationZonalAndTimeBinWaitingTime = new ArrayList<>();
    private final ArrayList<Map<Integer, DataStats[]>> iterationDistanceAndTimeBinDelayFactor = new ArrayList<>();
    private final FixedDrtZonalSystem zones;
    private final int timeBinSize_min;
    private final int distanceBinSize_m;
    private final int lastBinStartDistance_m;
    private static final Logger logger = Logger.getLogger(DrtFixedZoneMetrics.class);

    public DrtFixedZoneMetrics(FixedDrtZonalSystem zones, int timeBinSize_min, int distanceBinSize_m, int lastBinStartDistance_m) {
        this.zones = zones;
        this.timeBinSize_min = timeBinSize_min;
        this.distanceBinSize_m = distanceBinSize_m;
        this.lastBinStartDistance_m = lastBinStartDistance_m;

    }

    private static Map<String, Set<DrtTripData>[]> assignDrtTripsToZonesAndTimeBin(Set<DrtTripData> drtTrips, FixedDrtZonalSystem zones, int timeBinSize_min) {
        DrtTimeUtils timeUtils = new DrtTimeUtils(timeBinSize_min);
        int nTimeBins = timeUtils.getBinCount();

        Map<String, Set<DrtTripData>[]> drtTripsByZoneAndTimeBin = new HashMap<>();
        for (DrtTripData drtTrip : drtTrips) {
            String zone = zones.getZoneForLinkId(drtTrip.startLinkId);
            if (zone == null) {
                logger.warn("No zone found for link " + drtTrip.startLinkId);
                continue;
            }
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

    private static Map<Integer, Set<DrtTripData>[]> assignDrtTripsToDistanceAndTimeBin(Set<DrtTripData> drtTrips, int distanceBinSize_m,
                                                                                       int timeBinSize_min, int lastBinStartDistance_m) {
        DrtTimeUtils timeUtils = new DrtTimeUtils(timeBinSize_min);
        int nTimeBins = timeUtils.getBinCount();

        DrtDistanceBinUtils distanceUtils = new DrtDistanceBinUtils(distanceBinSize_m, lastBinStartDistance_m);
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

    public static Map<Integer, DataStats[]> calculateDistanceAndTimeBinDelayFactor(Set<DrtTripData> drtTripData, int distanceBinSize_m,
                                                                                   int timeBinSize_min, int lastBinStartDistance_m) {
        Map<Integer, DataStats[]> distanceAndTimeBinDelayFactor = new HashMap<>();
        Map<Integer, Set<DrtTripData>[]> assignedDrtTrips = assignDrtTripsToDistanceAndTimeBin(drtTripData,
                distanceBinSize_m, timeBinSize_min, lastBinStartDistance_m);

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

    private static void writeZonalAndTimeBinWaitingTime(Map<String, DataStats[]> zonalAndTimeBinWaitingTime,
                                                        String outputPath) throws IOException {
        BufferedWriter bw = IOUtils.getBufferedWriter(outputPath);
        String separator = ";";
        String header = DataStats.getCSVHeader(separator);
        header = "zone" + separator + "timeBin" + separator + header;
        bw.write(header);
        bw.newLine();
        for (String zone : zonalAndTimeBinWaitingTime.keySet()) {
            DataStats[] timeBinWaitingTime = zonalAndTimeBinWaitingTime.get(zone);
            for (int i = 0; i < timeBinWaitingTime.length; i++) {
                bw.write(zone + separator + i + separator + timeBinWaitingTime[i].getCSVLine(separator));
                bw.newLine();
            }
        }
        bw.flush();
        bw.close();
    }

    private static void writeDistanceAndTimeBinDelayFactor(Map<Integer, DataStats[]> distanceAndTimeBinDelayFactor,
                                                           String outputPath) throws IOException {
        BufferedWriter bw = IOUtils.getBufferedWriter(outputPath);
        String separator = ";";
        String header = DataStats.getCSVHeader(separator);
        header = "distanceBin" + separator + "timeBin" + separator + header;
        bw.write(header);
        bw.newLine();
        for (Integer distanceBin : distanceAndTimeBinDelayFactor.keySet()) {
            DataStats[] timeBinDelayFactor = distanceAndTimeBinDelayFactor.get(distanceBin);
            for (int i = 0; i < timeBinDelayFactor.length; i++) {
                bw.write(distanceBin + separator + i + separator + timeBinDelayFactor[i].getCSVLine(separator));
                bw.newLine();
            }
        }
        bw.flush();
        bw.close();
    }

    public void writeLastIteration(String outputPathWithoutFilename) {
        try {
            writeZonalAndTimeBinWaitingTime(iterationZonalAndTimeBinWaitingTime.get(iterationZonalAndTimeBinWaitingTime.size() - 1), outputPathWithoutFilename + "drt_zonalAndTimeBinWaitingTime.csv");
            writeDistanceAndTimeBinDelayFactor(iterationDistanceAndTimeBinDelayFactor.get(iterationDistanceAndTimeBinDelayFactor.size() - 1), outputPathWithoutFilename + "drt_distanceAndTimeBinDelayFactor.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getDelayFactorFromDistanceAndTimeBinMoving(int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                                             int movingWindow) {
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

    public double getDelayFactorFromDistanceAndTimeBinSuccessive(int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                                                 double msaWeight) {
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

    public double getDelayFactorFromDistanceAndTimeBinIteration(int distanceBin, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback) {
        int nIteration = iterationDistanceAndTimeBinDelayFactor.size() - 1;
        if (iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin) == null ||
                timeBin >= iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin).length ||
                Double.isNaN(iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback))) {
            return Double.NaN;
        }
        return iterationDistanceAndTimeBinDelayFactor.get(nIteration).get(distanceBin)[timeBin].getStat(feedback);
    }

    public double getWaitTimeFromZoneAndTimeBinMoving(String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                                      int movingWindow) {
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

    public double getWaitTimeFromZoneAndTimeBinSuccessive(String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                                          double msaWeight) {
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

    public double getWaitTimeFromZoneAndTimeBinIteration(String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback) {
        int nIteration = iterationZonalAndTimeBinWaitingTime.size() - 1;
        if (iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone) == null ||
                timeBin >= iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone).length ||
                Double.isNaN(iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone)[timeBin].getStat(feedback))) {
            return Double.NaN;
        }
        return iterationZonalAndTimeBinWaitingTime.get(nIteration).get(zone)[timeBin].getStat(feedback);
    }

    public void calculateAndAddMetrics(Set<DrtTripData> drtTripData) {
        iterationZonalAndTimeBinWaitingTime.add(calculateZonalAndTimeBinWaitingTime(drtTripData, this.zones, this.timeBinSize_min));
        iterationDistanceAndTimeBinDelayFactor.add(calculateDistanceAndTimeBinDelayFactor(drtTripData,
                this.distanceBinSize_m, this.timeBinSize_min, this.lastBinStartDistance_m));
    }
}
