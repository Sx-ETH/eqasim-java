package org.eqasim.core.components.drt.travel_times;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.eqasim.core.components.drt.config_group.*;
import org.eqasim.core.components.drt.travel_times.dynamic.DynamicWaitTimeMetrics;
import org.eqasim.core.components.drt.travel_times.smoothing.Markov;
import org.eqasim.core.components.drt.travel_times.smoothing.Smoothing;
import org.eqasim.core.components.drt.travel_times.zonal.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import java.util.Set;

public class TravelTimeUpdates implements IterationEndsListener, StartupListener, ShutdownListener {
    private static final Logger logger = Logger.getLogger(TravelTimeUpdates.class);
    @Inject
    private static ExperiencedPlansService experiencedPlansService;
    private final DrtTimeTracker trackedTimes;
    private final Config config;
    private final Network network;
    private final DrtPredictions drtPredictions;
    // These are the fields that we need in case we use a fixed zonal system
    private FixedDrtZonalSystem zones = null;
    private DrtFixedZoneMetrics fixedZoneMetrics = null;
    private DrtTimeUtils drtTimeUtils = null;
    private DrtDistanceBinUtils drtDistanceBinUtils = null;

    // These fields are needed in case we use a fixed zonal system as a fallback to compute the global stats
    private DataStats globalWaitingTime = null;
    private DataStats globalDelayFactor = null;

    private DynamicWaitTimeMetrics dynamicWaitTimeMetrics = null;
    private Smoothing smoothing;


    @Inject
    public TravelTimeUpdates(DrtTimeTracker trackedTimes,
                             Config config, DrtPredictions drtPredictions, Network network, Smoothing smoothing) {
        this.trackedTimes = trackedTimes;
        this.config = config;
        this.drtPredictions = drtPredictions;
        this.network = network;
        this.smoothing = smoothing;
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


    public double getTravelTime_sec(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseDelayFactor()) {
            DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
            double delayFactor;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Global) {
                delayFactor = this.globalDelayFactor.getStat(feedback);
            } else {
                Link startLink = this.network.getLinks().get(route.getStartLinkId());
                Link endLink = this.network.getLinks().get(route.getEndLinkId());
                double euclideanDistance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord());
                int distanceBin = this.drtDistanceBinUtils.getBinIndex(euclideanDistance);

                int timeBin = this.drtTimeUtils.getBinIndex(departureTime);
                delayFactor = this.smoothing.getDelayFactor(fixedZoneMetrics, distanceBin, timeBin, feedback);
            }
            if (Double.isNaN(delayFactor)) {
                //logger.warn("No delay factor for specific distance and time bin found. Falling back to global delay factor.");
                if (Double.isNaN(globalDelayFactor.getStat(feedback))) {
                    //logger.warn("No global delay factor found. Falling back to max travel time.");
                    return route.getMaxTravelTime();
                }
                return route.getDirectRideTime() * globalDelayFactor.getStat(feedback);
            }
            return route.getDirectRideTime() * delayFactor;
        }
        return route.getMaxTravelTime();
    }

    public DataStats getTravelTimeStats(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseDelayFactor()) {
            DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
            DataStats delayFactorStats;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Global) {
                delayFactorStats = this.globalDelayFactor;
            } else {
                Link startLink = this.network.getLinks().get(route.getStartLinkId());
                Link endLink = this.network.getLinks().get(route.getEndLinkId());
                double euclideanDistance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord());
                int distanceBin = this.drtDistanceBinUtils.getBinIndex(euclideanDistance);

                int timeBin = this.drtTimeUtils.getBinIndex(departureTime);
                delayFactorStats = Markov.getDelayFactorStats(fixedZoneMetrics, distanceBin, timeBin);
            }
            if (Double.isNaN(delayFactorStats.getStat(feedback))) {
                return globalDelayFactor;
            }
            return delayFactorStats;
        }
        return new DataStats(route.getMaxTravelTime());
    }

    public double getWaitTime_sec(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseWaitTime()) {
            DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
            double waitTime = Double.NaN;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Global) {
                waitTime = this.globalWaitingTime.getStat(feedback);
            } else {
                int timeBin = this.drtTimeUtils.getBinIndex(departureTime);
                String zone = zones.getZoneForLinkId(route.getStartLinkId());
                if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {
                    waitTime = this.smoothing.getZonalWaitTime(fixedZoneMetrics, zone, timeBin, feedback);
                } else if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
                    //get type of dynamic system
                    DrtDynamicSystemParamSet dynamicParams = drtDmcConfig.getDrtMetricCalculationParamSet().getDrtDynamicSystemParamSet();
                    DrtDynamicSystemParamSet.Type dynamicType = dynamicParams.getType();
                    int kValue = dynamicParams.getKvalue();
                    double radius = dynamicParams.getRadius();
                    double kShare = dynamicParams.getkShare();
                    int kMax = dynamicParams.getkMax(); //ToDo need to come up with a suitable reason for choosing a max number of Kvalue
                    DrtDynamicSystemParamSet.DecayType decayType = dynamicParams.getDecayType();

                    DataStats avgWaitTimeIterStats = dynamicWaitTimeMetrics.getDynamicWaitTimeForTimeBin(route, timeBin, dynamicType, kValue, radius, kShare, kMax, decayType);
                    double avgWaitTimeIter = avgWaitTimeIterStats.getStat(feedback);
                    waitTime = this.smoothing.getDynamicWaitTime(avgWaitTimeIter, fixedZoneMetrics, zone, timeBin, feedback);


                }

            }
            if (Double.isNaN(waitTime)) {
                //logger.warn("No waiting time data for specific zone and time bin found, falling back to global waiting time");
                if (Double.isNaN(globalWaitingTime.getStat(feedback))) {
                    //logger.warn("No global waiting time data, returning maxWaitTime");
                    return route.getMaxWaitTime();
                }
                return globalWaitingTime.getStat(feedback);
            }
            return waitTime;
        }
        return route.getMaxWaitTime();
    }

    public DataStats getWaitTimeStats(DrtRoute route, double departureTime) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        if (drtDmcConfig.isUseWaitTime()) {
            DrtModeChoiceConfigGroup.Feedback feedback = drtDmcConfig.getFeedBackMethod();
            DataStats waitTimeStats = new DataStats();
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Global) {
                waitTimeStats = this.globalWaitingTime;
            } else {
                int timeBin = this.drtTimeUtils.getBinIndex(departureTime);
                String zone = zones.getZoneForLinkId(route.getStartLinkId());
                if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.ZonalSystem) {
                    waitTimeStats = Markov.getZonalWaitTimeStats(fixedZoneMetrics, zone, timeBin);
                } else if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
                    //get type of dynamic system
                    DrtDynamicSystemParamSet dynamicParams = drtDmcConfig.getDrtMetricCalculationParamSet().getDrtDynamicSystemParamSet();
                    DrtDynamicSystemParamSet.Type dynamicType = dynamicParams.getType();
                    int kValue = dynamicParams.getKvalue();
                    double radius = dynamicParams.getRadius();
                    double kShare = dynamicParams.getkShare();
                    int kMax = dynamicParams.getkMax(); //ToDo need to come up with a suitable reason for choosing a max number of Kvalue
                    DrtDynamicSystemParamSet.DecayType decayType = dynamicParams.getDecayType();

                    DataStats avgWaitTimeIterStats = dynamicWaitTimeMetrics.getDynamicWaitTimeForTimeBin(route, timeBin, dynamicType, kValue, radius, kShare, kMax, decayType);
                    waitTimeStats = Markov.getDynamicWaitTimeStats(avgWaitTimeIterStats, fixedZoneMetrics, zone, timeBin);


                }

            }
            if (Double.isNaN(waitTimeStats.getStat(feedback))) {
                return globalWaitingTime;
            }
            return waitTimeStats;
        }
        return new DataStats(route.getMaxWaitTime());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        Set<DrtTripData> drtTrips = trackedTimes.getDrtTrips();

        // We always compute the global stats
        this.globalWaitingTime = DrtFixedZoneMetrics.calculateGlobalWaitingTime(drtTrips);
        this.globalDelayFactor = DrtFixedZoneMetrics.calculateGlobalDelayFactor(drtTrips);

        // Calculate the zonal and time bin metrics and append them to the ArrayLists, we always compute it
        // because the DF is always computed in a zonal system (in case we don't use global)
        if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() != DrtMetricCalculationParamSet.Method.Global) {
            this.fixedZoneMetrics.calculateAndAddMetrics(drtTrips);
        }

        if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
            dynamicWaitTimeMetrics.prepareDynamicLocationsAndTimeBins(drtTrips);
        }

        if (drtDmcConfig.writeDetailedStats()) {
            try {
                String outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                        "drt_drtTripsStats.csv");
                writeDrtTripsStats(drtTrips, outputPath);
                outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
                        "drt_globalStats.csv");
                writeGlobalStats(this.globalWaitingTime, this.globalDelayFactor, outputPath);
                if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() != DrtMetricCalculationParamSet.Method.Global) {
                    String pathWithoutFileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "");
                    this.fixedZoneMetrics.writeLastIteration(pathWithoutFileName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.drtPredictions.writeTripsPredictions(event.getIteration(), this.config);
            this.drtPredictions.clearTripPredictions();
        }

        if (drtDmcConfig.getDrtMetricSmootheningParamSet().getSmootheningType() != DrtMetricSmootheningParamSet.SmootheningType.Markov) {
            throw new RuntimeException("Only Markov smoothening is supported at the moment for the simulation of the trips");
        }
        String filename = "drt_simulatedTrips.csv";
        String outputDir = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), filename);
        BufferedWriter writer = IOUtils.getBufferedWriter(outputDir);
        try {
            String header = "";//""personId;startTime;startLink;endLink;real_waiTime;real_travelTime;predicted_waitTime;predicted_travelTime;real_waiTime;real_travelTime\n";
            header += "personId;";
            header += "startTime;";
            header += "startLink;";
            header += "endLink;";
            header += "waitTime_real;";
            header += "travelTime_real;";
            header += "waitTime_predicted;";
            header += "travelTime_predicted;";
            header += "waitTime_avg;";
            header += "waitTime_std;";
            header += "waitTime_weightedAvg;";
            header += "waitTime_weightedStd;";
            header += "waitTime_min;";
            header += "waitTime_p5;";
            header += "waitTime_p25;";
            header += "waitTime_median;";
            header += "waitTime_p75;";
            header += "waitTime_p95;";
            header += "waitTime_max;";
            header += "waitTime_nTrips;";

            header += "delayFactor_avg;";
            header += "delayFactor_std;";
            header += "delayFactor_weightedAvg;";
            header += "delayFactor_weightedStd;";
            header += "delayFactor_min;";
            header += "delayFactor_p5;";
            header += "delayFactor_p25;";
            header += "delayFactor_median;";
            header += "delayFactor_p75;";
            header += "delayFactor_p95;";
            header += "delayFactor_max;";
            header += "delayFactor_nTrips\n";

            writer.write(header);
            // Simulate the replanning to get the predicted values
            for (DrtTripData drtTrip : drtTrips) {
                DrtRoute route = generateRouteFromTripData(drtTrip);
                double departureTime = drtTrip.startTime;
                double waitTime_sec = getWaitTime_sec(route, departureTime);
                double travelTime_sec = getTravelTime_sec(route, departureTime);
                DataStats waitTimeStats = getWaitTimeStats(route, departureTime);
                DataStats travelTimeStats = getTravelTimeStats(route, departureTime);

                writer.write(drtTrip.personId + ";");
                writer.write(drtTrip.startTime + ";");
                writer.write(drtTrip.startLinkId + ";");
                writer.write(drtTrip.endLinkId + ";");
                writer.write(drtTrip.waitTime + ";");
                writer.write(drtTrip.totalTravelTime + ";");
                writer.write(waitTime_sec + ";");
                writer.write(travelTime_sec + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.average) + ";");
                writer.write(waitTimeStats.getStd() + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.weightedAverage) + ";");
                writer.write(waitTimeStats.getWeightedStd() + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.min) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_5) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_25) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.median) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_75) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_95) + ";");
                writer.write(waitTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.max) + ";");
                writer.write(waitTimeStats.getNTrips() + ";");

                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.average) + ";");
                writer.write(travelTimeStats.getStd() + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.weightedAverage) + ";");
                writer.write(travelTimeStats.getWeightedStd() + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.min) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_5) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_25) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.median) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_75) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.p_95) + ";");
                writer.write(travelTimeStats.getStat(DrtModeChoiceConfigGroup.Feedback.max) + ";");
                writer.write(travelTimeStats.getNTrips() + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DrtRoute generateRouteFromTripData(DrtTripData drtTrip) {
        DrtRoute route = new DrtRoute(drtTrip.startLinkId, drtTrip.endLinkId);
        route.setDirectRideTime(drtTrip.estimatedUnsharedTime);
        route.setMaxWaitTime(600);
        route.setTravelTime(240 + 1.5 * drtTrip.estimatedUnsharedTime);
        return route;
    }

    // We need to create the zones on startup in case we use the zonal average wait time
    @Override
    public void notifyStartup(StartupEvent event) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);

        if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() != DrtMetricCalculationParamSet.Method.Global) {
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

            // We prepare the dynamicMetrics in case we want to use them
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getSpatialType() == DrtMetricCalculationParamSet.SpatialType.DynamicSystem) {
                this.dynamicWaitTimeMetrics = new DynamicWaitTimeMetrics(this.network, timeBinSize_min);
            }

            // We prepare the fixedZoneMetrics because we will use them in any case (the DF is always calculated with them)
            int distanceBinSize_m;
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.Spatio ||
                    drtDmcConfig.getDrtMetricCalculationParamSet().getMethod() == DrtMetricCalculationParamSet.Method.SpatioTemporal) {
                distanceBinSize_m = drtDmcConfig.getDrtMetricCalculationParamSet().getDistanceBinMetres();
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
            // In case we don't do spatio analysis, we have to create a zonal system with one zone and one distance bin (0-maxDistance)
            else {
                this.zones = new SingleZoneDrtZonalSystem(network);
                distanceBinSize_m = -1;
            }
            int lastBinStartDistance_m = drtDmcConfig.getDrtMetricCalculationParamSet().getLastBinStartDistance_m();
            this.fixedZoneMetrics = new DrtFixedZoneMetrics(this.zones, timeBinSize_min, distanceBinSize_m,
                    lastBinStartDistance_m);
            this.drtDistanceBinUtils = new DrtDistanceBinUtils(distanceBinSize_m, lastBinStartDistance_m);
            this.drtTimeUtils = new DrtTimeUtils(timeBinSize_min);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        if (this.zones != null) {
            String fileName = event.getServices().getControlerIO().getOutputFilename("drt_link2FixedZones.csv");
            zones.writeLink2Zone(fileName);

            String crs = ((GlobalConfigGroup) config.getModules().get(GlobalConfigGroup.GROUP_NAME)).getCoordinateSystem();

            fileName = event.getServices().getControlerIO().getOutputFilename("drt_FixedZones.shp");

            Collection<SimpleFeature> features = this.zones.getSimpleFeatures(crs);
            ShapeFileWriter.writeGeometries(features, fileName);
        }
    }
}
