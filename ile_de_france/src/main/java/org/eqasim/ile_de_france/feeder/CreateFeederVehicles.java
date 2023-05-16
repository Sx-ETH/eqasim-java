package org.eqasim.ile_de_france.feeder;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.io.*;
import java.util.*;

public class CreateFeederVehicles {
    private final static Logger logger = Logger.getLogger(CreateFeederVehicles.class);

    public static final String STOPS_USAGES_STOP_ID_COLUMN = "stop_id";
    public static final String STOPS_USAGES_NB_ACCESSES_COLUMN = "nb_accesses";
    public static final String STOPS_USAGES_NB_EGRESSES_COLUMN = "nb_egresses";

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("network-path", "output-vehicles-path", "vehicles-number", "schedule-path", "stations-usages-path")
                .allowOptions("vehicles-capacity", "service-begin-time", "service-end-time", "max-stations")
                .build();

        int vehiclesNumber = Integer.parseInt(cmd.getOptionStrict("vehicles-number"));

        int vehiclesCapacity = cmd.hasOption("vehicles-capacity") ? Integer.parseInt(cmd.getOptionStrict("vehicles-capacity")) : 4;
        int serviceBeginTime = cmd.hasOption("service-begin-time") ? Integer.parseInt(cmd.getOptionStrict("service-begin-time")) : 0;
        int serviceEndTime = cmd.hasOption("service-end-time") ? Integer.parseInt(cmd.getOptionStrict("service-end-time")) : 24 * 3600;
        int maxStations = cmd.hasOption("max-stations") ? Integer.parseInt(cmd.getOptionStrict("max-stations")) : -1;
        Network network = NetworkUtils.createNetwork();
        Network filteredNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));
        new TransportModeNetworkFilter(network).filter(filteredNetwork, Collections.singleton("car"));

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
        IdMap<TransitStopFacility, Id<Link>> transitStopFacilityIdToLinkId = new IdMap<>(TransitStopFacility.class);
        IdMap<TransitStopFacility, Integer> vehiclesNumberByFacility = new IdMap<>(TransitStopFacility.class);
        IdMap<TransitStopFacility, Double> proportions = new IdMap<>(TransitStopFacility.class);


        TransitSchedule schedule = scenario.getTransitSchedule();

        String stationsUsagesPath = cmd.getOptionStrict("stations-usages-path");
        String line;
        List<String> header = null;

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(stationsUsagesPath)));
        int sumWeights = 0;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                String stopId = row.get(header.indexOf(STOPS_USAGES_STOP_ID_COLUMN));
                Id<TransitStopFacility> transitStopFacilityId = Id.create(stopId, TransitStopFacility.class);
                TransitStopFacility transitStopFacility = schedule.getFacilities().get(transitStopFacilityId);
                if(transitStopFacility == null) {
                    continue;
                }
                Id<Link> linkId = NetworkUtils.getNearestLink(filteredNetwork, transitStopFacility.getCoord()).getId();
                transitStopFacilityIdToLinkId.put(transitStopFacilityId, linkId);
                logger.info("Vehicles for facility " + transitStopFacilityId.toString() + "("+ transitStopFacility.getName()+") will be located on link " + linkId.toString());
                double weight = Double.parseDouble(row.get(header.indexOf(STOPS_USAGES_NB_ACCESSES_COLUMN))) + Double.parseDouble(row.get(header.indexOf(STOPS_USAGES_NB_EGRESSES_COLUMN)));
                proportions.put(transitStopFacilityId, weight);
            }
        }
        reader.close();
        List<Id<TransitStopFacility>> facilities = new ArrayList<>(proportions.keySet());
        if(maxStations > -1 && facilities.size() > maxStations) {
            Collections.sort(facilities, Comparator.comparingDouble(proportions::get));
            facilities = facilities.subList(facilities.size()-maxStations, facilities.size());
        }
        for(Id<TransitStopFacility> transitStopFacilityId: facilities) {
            sumWeights+=proportions.get(transitStopFacilityId);
        }
        logger.info("Identified " + facilities.size() + " relevant facilities");
        if(vehiclesNumber < facilities.size()){
            throw new IllegalStateException(vehiclesNumber + " vehicles are not enough to guarantee at least one vehicle per facility with " + facilities.size() + " facilities");
        }
        int addedVehicles = 0;
        for(Id<TransitStopFacility> transitStopFacilityId: facilities) {
            double ratio = proportions.get(transitStopFacilityId) / sumWeights;
            int facilityVehiclesNumber = 1 + (int) ((vehiclesNumber - facilities.size()) * ratio);
            addedVehicles+=facilityVehiclesNumber;
            vehiclesNumberByFacility.put(transitStopFacilityId, facilityVehiclesNumber);
            logger.info(String.format("Facility %s (%s) gets %d vehicles %f%%", transitStopFacilityId.toString(), schedule.getFacilities().get(transitStopFacilityId).getName(), facilityVehiclesNumber, ratio*100));
        }
        assert addedVehicles == vehiclesNumber;

        FleetSpecification fleetSpecification = new FleetSpecificationImpl();
        int i=0;
        for(Id<TransitStopFacility> transitStopFacilityId: facilities) {
            generateVehicles(i, i+vehiclesNumberByFacility.get(transitStopFacilityId), transitStopFacilityIdToLinkId.get(transitStopFacilityId), serviceBeginTime, serviceEndTime, vehiclesCapacity, fleetSpecification);
            i += vehiclesNumberByFacility.get(transitStopFacilityId);
        }

        while(i<vehiclesNumber) {
            for(Id<TransitStopFacility> transitStopFacilityId: transitStopFacilityIdToLinkId.keySet()) {
                generateVehicles(i, i+1, transitStopFacilityIdToLinkId.get(transitStopFacilityId), serviceBeginTime, serviceEndTime, vehiclesCapacity, fleetSpecification);
                i++;
                if(i>=vehiclesCapacity) {
                    break;
                }
            }
        }


        new FleetWriter(fleetSpecification.getVehicleSpecifications().values().stream()).write(cmd.getOptionStrict("output-vehicles-path"));
    }

    private static void generateVehicles(int startVehicleIndex, int endVehicleIndex, Id<Link> linkId, int serviceBeginTime, int serviceEndTime, int vehicleCapacity, FleetSpecification fleetSpecification) {
        for(int i=startVehicleIndex; i<endVehicleIndex; i++) {
            Id<DvrpVehicle> vehicleId = Id.create("vehicle_drt_"+i, DvrpVehicle.class);
            logger.info("Creating vehicle " + vehicleId.toString() + " on link " + linkId.toString());
            DvrpVehicleSpecification dvrpVehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder().id(vehicleId).startLinkId(linkId).serviceBeginTime(serviceBeginTime).serviceEndTime(serviceEndTime).capacity(vehicleCapacity).build();
            fleetSpecification.addVehicleSpecification(dvrpVehicleSpecification);
        }
    }
}
