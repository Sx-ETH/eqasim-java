package org.eqasim.switzerland.utils;

import org.eqasim.core.components.drt.utils.CreateDrtVehicles;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class GenerateVehiclesForFixedDemand {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("populationFile", "outputDirectory")//
                .requireOptions("networkFile")
                .allowOptions("boundaryShapefile")//
                .build();

        String populationFile = cmd.getOptionStrict("populationFile");
        String outputDirectory = cmd.getOptionStrict("outputDirectory");

        String networkFile = cmd.getOptionStrict("networkFile");

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);
        new PopulationReader(scenario).readFile(populationFile);

        ScenarioUtils.loadScenario(scenario);
        Population populationData = scenario.getPopulation();

        System.out.println("Starting creating DRT vehicles");

        double[] nrOfTripPerVehicle = {10, 20, 30, 40, 50, 75};

        int drtDemand = Integer.parseInt(populationFile.split("_")[2]);
        System.out.println("drtDemand: " + drtDemand);

        for (double nrOfTripsPerVehicle : nrOfTripPerVehicle) {
            String vehiclesOutputFile = outputDirectory + "/fixed_demand_" + drtDemand + "_" + nrOfTripsPerVehicle + "_drt_vehicles.xml";
            int nrOfVehicles = (int) Math.ceil(drtDemand / nrOfTripsPerVehicle);
            String vehicle_name = "drt";
            int seats = 4;
            double operationStartTime = 0;
            double operationEndTime = 24 * 3600;
            System.out.println("Number of vehicles: " + nrOfVehicles);
            CreateDrtVehicles createDrtVehicles = new CreateDrtVehicles(network, nrOfVehicles, operationStartTime, operationEndTime, seats);
            boolean isUseBoundary = cmd.hasOption("boundaryShapefile");
            if (isUseBoundary) {
                createDrtVehicles.setUseBoundary(true);
                String boundaryPolygon = cmd.getOption("boundaryShapefile").get();
                URL boundaryUrl = new File(boundaryPolygon).toURI().toURL();
                createDrtVehicles.generateBoundary(boundaryUrl);
            }
            createDrtVehicles.createVehicles(populationData, vehiclesOutputFile, vehicle_name);
        }
    }
}
