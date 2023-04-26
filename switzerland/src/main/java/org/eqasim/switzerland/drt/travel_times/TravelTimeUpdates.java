package org.eqasim.switzerland.drt.travel_times;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.eqasim.switzerland.drt.config_group.DrtMetricCalculationParamSet;
import org.eqasim.switzerland.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.switzerland.drt.config_group.DrtZonalSystemParamSet;
import org.eqasim.switzerland.drt.travel_times.zonal.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TravelTimeUpdates implements IterationEndsListener, StartupListener, ShutdownListener {
    private static final Logger logger = Logger.getLogger(TravelTimeUpdates.class);
    @Inject
    private static ExperiencedPlansService experiencedPlansService;
    private final DrtTimeTracker trackedTimes;
    private final Config config;
    private final DrtPredictions drtPredictions;
    // These are the fields that we need in case we use a fixed zonal system
    private FixedDrtZonalSystem zones = null;
    private Map<String, DataStats[]> zonalAndTimeBinWaitingTime = null;
    private Map<Integer, DataStats[]> distanceAndTimeBinDelayFactor = null;
    // These fields are needed in case we use a fixed zonal system as a fallback to compute the global stats
    private DataStats globalWaitingTime = null;
    private DataStats globalDelayFactor = null;


    @Inject
    public TravelTimeUpdates(DrtTimeTracker trackedTimes,
                             Config config, DrtPredictions drtPredictions) {
        this.trackedTimes = trackedTimes;
        this.config = config;
        this.drtPredictions = drtPredictions;
    }

    private static void writeGlobalStats(DataStats globalWaitingTime, DataStats globalDelayFactor, String outputPath)
            throws IOException {
        BufferedWriter bw = IOUtils.getBufferedWriter(outputPath);
        String separator = ";";
        String header = DataStats.getCSVHeader(separator);
        header = "metric" + separator + header;
        bw.write(header);
        bw.newLine();
        bw.write("globalWaitingTime" + separator + globalWaitingTime.getCSVLine(separator));
        bw.newLine();
        bw.write("globalDelayFactor" + separator + globalDelayFactor.getCSVLine(separator));
        bw.flush();
        bw.close();
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

    private static int getTripIndexFromDrtLeg(Id<Person> personId, double startTime) {
        Plan personPlan = experiencedPlansService.getExperiencedPlans().get(personId);
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(personPlan);
        for (int i = 0; i < trips.size(); i++) { // We start at 0 to be able to match it with the predictions
            TripStructureUtils.Trip trip = trips.get(i);
            for (Leg leg : trip.getLegsOnly()) {
                if (leg.getMode().equals("drt") && leg.getDepartureTime().seconds() == startTime) {
                    return i;
                }
            }
        }
        logger.warn("Could not find tripId for drt leg of person " + personId + " at time " + startTime);
        return -1;

    }

    private static void writeDrtTripsStats(Set<DrtTripData> drtTrips, String outputPath)
            throws IOException {
        String delimiter = ";";
        BufferedWriter bw = IOUtils.getBufferedWriter(outputPath);
        bw.write("personId" + delimiter + "tripIndex" + delimiter + "startTime" + delimiter + "arrivalTime" +
                delimiter + "totalTravelTime" + delimiter + "routerUnsharedTime" + delimiter + "estimatedUnsharedTime"
                + delimiter + "delayFactor" + delimiter + "waitTime" + delimiter + "startX" + delimiter + "startY"
                + delimiter + "endX" + delimiter + "endY" + delimiter + "euclideanDistance");

        for (DrtTripData drtTrip : drtTrips) {
            // print out to csv, the estimatedUnsharedTime from requestSubmissionEvent
            // and compare with this computed one from the route

            // We first find the id of the trip to be able to match it with the predictions
            int tripIndex = getTripIndexFromDrtLeg(drtTrip.personId, drtTrip.startTime);

            bw.newLine();

            double delayFactor;
            if (drtTrip.routerUnsharedTime > 0) {
                delayFactor = drtTrip.totalTravelTime / drtTrip.routerUnsharedTime;
            } else {
                delayFactor = drtTrip.totalTravelTime / drtTrip.estimatedUnsharedTime;
            }
            double euclideanDistance = CoordUtils.calcEuclideanDistance(drtTrip.startCoord, drtTrip.endCoord);

            bw.write(drtTrip.personId + delimiter + tripIndex + delimiter + drtTrip.startTime + delimiter
                    + drtTrip.arrivalTime + delimiter + drtTrip.totalTravelTime + delimiter
                    + drtTrip.routerUnsharedTime + delimiter + drtTrip.estimatedUnsharedTime + delimiter + delayFactor
                    + delimiter + drtTrip.waitTime + delimiter + drtTrip.startCoord.getX() + delimiter
                    + drtTrip.startCoord.getY() + delimiter + drtTrip.endCoord.getX() + delimiter
                    + drtTrip.endCoord.getY() + delimiter + euclideanDistance);

        }

        bw.flush();
        bw.close();
    }

    private double getRideTimeFromDistanceAndTimeBin(int distanceBin, int timeBin,
                                                     DrtModeChoiceConfigGroup.Feedback feedback,
                                                     double directRideTime, double maxTravelTime) {
        if (distanceAndTimeBinDelayFactor.get(distanceBin) == null ||
                timeBin >= distanceAndTimeBinDelayFactor.get(distanceBin).length ||
                Double.isNaN(distanceAndTimeBinDelayFactor.get(distanceBin)[timeBin].getStat(feedback))
        ) {
            logger.warn("No delay factor for distance bin " + distanceBin + " and time bin " + timeBin +
                    " found. Falling back to global delay factor.");
            if (Double.isNaN(globalDelayFactor.getStat(feedback))) {
                logger.warn("No global delay factor found. Falling back to max travel time.");
                return maxTravelTime;
            }
            return directRideTime * globalDelayFactor.getStat(feedback);
        }
        return directRideTime * distanceAndTimeBinDelayFactor.get(distanceBin)[timeBin].getStat(feedback);
    }

    public double getTravelTime_sec(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseDelayFactor()) {
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {
                DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
                DrtDistanceBinUtils drtDistanceBinUtils = new DrtDistanceBinUtils(drtDmcConfig.getDrtMetricCalculationParamSet().getDistanceBinMetres());
                int distanceBin = drtDistanceBinUtils.getBinIndex(route.getDistance());

                DrtTimeUtils drtTimeUtils = new DrtTimeUtils(drtDmcConfig.getDrtMetricCalculationParamSet().getTimeBinMin());
                int timeBin = drtTimeUtils.getBinIndex(departureTime);
                return getRideTimeFromDistanceAndTimeBin(distanceBin, timeBin, feedback, route.getDirectRideTime(), route.getMaxTravelTime());
            } else if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
                throw new RuntimeException("Not implemented yet.");
            }
        }
        return route.getMaxTravelTime(); // toDo now alpha*time + beta, alpha and beta defined in drt config module
    }

    private double getWaitTimeFromZoneAndTimeBin(String zone, int timeBin, DrtModeChoiceConfigGroup.Feedback feedback,
                                                 double maxWaitTime) {
        if (zonalAndTimeBinWaitingTime.get(zone) == null || timeBin >= zonalAndTimeBinWaitingTime.get(zone).length
                || Double.isNaN(zonalAndTimeBinWaitingTime.get(zone)[timeBin].getStat(feedback))) {
            logger.warn("No waiting time data for zone " + zone + " and time bin " + timeBin +
                    ", falling back to global waiting time");
            if (Double.isNaN(globalWaitingTime.getStat(feedback))) {
                logger.warn("No global waiting time data, returning maxWaitTime");
                return maxWaitTime;
            }
            return globalWaitingTime.getStat(feedback);
        }
        return zonalAndTimeBinWaitingTime.get(zone)[timeBin].getStat(feedback);

    }

    public double getWaitTime_sec(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseWaitTime()) {
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {
                DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
                String zone = zones.getZoneForLinkId(route.getStartLinkId());
                DrtTimeUtils drtTimeUtils = new DrtTimeUtils(drtDmcConfig.getDrtMetricCalculationParamSet().getTimeBinMin());
                int timeBin = drtTimeUtils.getBinIndex(departureTime);
                return getWaitTimeFromZoneAndTimeBin(zone, timeBin, feedback, route.getMaxWaitTime());
            } else if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
                throw new RuntimeException("Not implemented yet.");
            }
        }
        return route.getMaxWaitTime();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        Set<DrtTripData> drtTrips = trackedTimes.getDrtTrips();

        // We always compute the global stats
        this.globalWaitingTime = DrtFixedZoneMetrics.calculateGlobalWaitingTime(drtTrips);
        this.globalDelayFactor = DrtFixedZoneMetrics.calculateGlobalDelayFactor(drtTrips);

        if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {


            // In case we don't do temporal analysis we need to use one time bin (0-endTime)
            int timeBinSize_min;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.SpatioTemporal ||
                    drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Temporal) {
                timeBinSize_min = drtDmcConfig.getDrtMetricCalculationParamSet().getTimeBinMin();
            } else {
                QSimConfigGroup qSimConfigGroup = config.qsim();
                double endTime_s = qSimConfigGroup.getEndTime().seconds();
                timeBinSize_min = (int) (endTime_s / 60);
            }
            // In case we don't do zonal analysis we need to use one distance bin (0-maxDistance)
            int distanceBinSize_m;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.SpatioTemporal ||
                    drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Spatio) {
                distanceBinSize_m = drtDmcConfig.getDrtMetricCalculationParamSet().getDistanceBinMetres();
            } else {
                distanceBinSize_m = -1;
            }
            // Calculate the zonal and time bin metrics
            this.zonalAndTimeBinWaitingTime = DrtFixedZoneMetrics.calculateZonalAndTimeBinWaitingTime(drtTrips,
                    this.zones, timeBinSize_min);
            this.distanceAndTimeBinDelayFactor = DrtFixedZoneMetrics.calculateDistanceAndTimeBinDelayFactor(drtTrips,
                    distanceBinSize_m, timeBinSize_min);
        } else if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
            throw new RuntimeException("Not implemented yet");
        }
        if (drtDmcConfig.writeDetailedStats()) {
            try {
                String outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                        "drt_drtTripsStats.csv");
                writeDrtTripsStats(drtTrips, outputPath);
                outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                        "drt_globalStats.csv");
                writeGlobalStats(this.globalWaitingTime, this.globalDelayFactor, outputPath);
                if (this.zonalAndTimeBinWaitingTime != null) {
                    outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                            "drt_zonalAndTimeBinWaitingTime.csv");
                    writeZonalAndTimeBinWaitingTime(this.zonalAndTimeBinWaitingTime, outputPath);
                }
                if (this.distanceAndTimeBinDelayFactor != null) {
                    outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                            "drt_distanceAndTimeBinDelayFactor.csv");
                    writeDistanceAndTimeBinDelayFactor(this.distanceAndTimeBinDelayFactor, outputPath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.drtPredictions.writeTripsPredictions(event.getIteration(), this.config);
            this.drtPredictions.clearTripPredictions();
        }

    }

    // We need to create the zones on startup in case we use the zonal average wait time
    @Override
    public void notifyStartup(StartupEvent event) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        Network network = event.getServices().getScenario().getNetwork();

        if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {
            if (drtDmcConfig.isUseWaitTime() &&
                    (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Spatio ||
                            drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.SpatioTemporal)) {
                DrtZonalSystemParamSet zonalSystemParamSet = drtDmcConfig.getDrtMetricCalculationParamSet().getDrtZonalSystemParamSet();
                if (zonalSystemParamSet.getZonesGeneration() == DrtZonalSystemParamSet.ZonesGeneration.GridFromNetwork) {
                    double cellSize = zonalSystemParamSet.getCellSize();
                    if (zonalSystemParamSet.getZoneShape() == DrtZonalSystemParamSet.ZoneShape.Hexagon) {
                        this.zones = new HexGridDrtZonalSystem(network, cellSize);
                    } else if (zonalSystemParamSet.getZoneShape() == DrtZonalSystemParamSet.ZoneShape.Square) {
                        this.zones = new SquareGridDrtZonalSystem(network, cellSize);
                    } else {
                        throw new IllegalArgumentException("Zone shape not implemented yet!");
                    }
                } else if (!zonalSystemParamSet.getShapefile().isBlank()) {
                    this.zones = new TAZDrtZonalSystem(network, zonalSystemParamSet.getShapefile());
                } else {
                    throw new IllegalArgumentException("Zonal system type not implemented yet!\n" +
                            "Please use either GridFromNetwork on ZonesGeneration or specify a Shapefile.");
                }

            }
            // In case we don't do spatio analysis, we have to create a zonal system with one zone
            else {
                this.zones = new SingleZoneDrtZonalSystem(network);
            }
        } else {
            throw new IllegalArgumentException("Spatial type not implemented yet!");
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        String fileName = event.getServices().getControlerIO().getOutputFilename("drt_link2FixedZones.csv");
        zones.writeLink2Zone(fileName);

        String crs = ((GlobalConfigGroup) config.getModules().get(GlobalConfigGroup.GROUP_NAME)).getCoordinateSystem();

        fileName = event.getServices().getControlerIO().getOutputFilename("drt_FixedZones.shp");

        Collection<SimpleFeature> features = this.zones.getSimpleFeatures(crs);
        ShapeFileWriter.writeGeometries(features, fileName);
    }
}
