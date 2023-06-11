package org.eqasim.switzerland.utils;

import org.eqasim.core.components.drt.utils.CreateDrtVehicles;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GenerateFixedDRTDemand {

    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("populationFile", "outputDirectory", "drtDemand")//
                .requireOptions("networkFile", "tripsPerVehicle")
                .allowOptions("boundaryShapefile")//
                .build();

        String populationFile = cmd.getOptionStrict("populationFile");
        String outputDirectory = cmd.getOptionStrict("outputDirectory");
        int drtDemand = Integer.parseInt(cmd.getOptionStrict("drtDemand"));

        String networkFile = cmd.getOptionStrict("networkFile");

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);
        new PopulationReader(scenario).readFile(populationFile);

        ScenarioUtils.loadScenario(scenario);
        Population populationData = scenario.getPopulation();

        List<Id<Person>> allPersons = new ArrayList<>();

        List<Leg> candidateLegs = new ArrayList<>();
        for (Person person : populationData.getPersons().values()) {
            allPersons.add(person.getId());
            Plan selectedPlan = person.getSelectedPlan();
            for (PlanElement pe : selectedPlan.getPlanElements()) {
                if (pe instanceof Leg) {
                    Leg leg = (Leg) pe;
                    Activity startAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().indexOf(leg) - 1);
                    Activity endAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().indexOf(leg) + 1);
                    double distance = Math.sqrt(Math.pow(startAct.getCoord().getX() - endAct.getCoord().getX(), 2) + Math.pow(startAct.getCoord().getY() - endAct.getCoord().getY(), 2));
                    if (leg.getMode().equals("car") && distance >= 250) {
                        candidateLegs.add(leg);
                    }
                }
            }
        }
        System.out.println(candidateLegs.size() + " legs found");

        Collections.shuffle(candidateLegs, ThreadLocalRandom.current());

        for (int i = 0; i < drtDemand; i++) {
            Leg leg = candidateLegs.get(i);
            leg.setMode("drt");
        }

        String outputfile = outputDirectory + "/fixed_demand_" + drtDemand + "_population.xml.gz";
        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished outputting population file");
        System.out.println("Starting creating DRT vehicles");

        String vehiclesOutputFile = outputDirectory + "/fixed_demand_" + drtDemand + "_drt_vehicles.xml.gz";
        int tripsPerVehicle = Integer.parseInt(cmd.getOptionStrict("tripsPerVehicle"));

        String vehicle_name = "drt";
        int seats = 4;
        double operationStartTime = 0;
        double operationEndTime = 24 * 3600;

        int numberOfVehicles = (int) Math.ceil(((double) drtDemand) / tripsPerVehicle);
        System.out.println("Number of vehicles: " + numberOfVehicles);

        CreateDrtVehicles createDrtVehicles = new CreateDrtVehicles(network, numberOfVehicles, operationStartTime, operationEndTime, seats);

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