package org.eqasim.switzerland.drt.utils;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.Collection;

public class RunCreateDrtVehicles {
    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("networkFile", "outputpath", "fleetSize", "operationStartTime", "operationEndTime", "seats")
                .allowOptions("populationFile", "name_suffix", "identifier", "boundary-shapefile", "random-seed")//
                .build();

        String nameSuffix = cmd.getOption("name_suffix").isPresent() ? cmd.getOption("name_suffix").get() : "drt_vehicles";
        //identifier to specify type of generation if pop density, random or by hubs

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        boolean isUseBoundary = cmd.getOption("boundary-shapefile").isPresent();
        if (isUseBoundary) {
            String boundaryPolygon = cmd.getOption("boundary-shapefile").get();
            ShapeFileReader shapeFileReader = new ShapeFileReader();
            Collection<SimpleFeature> zurich_shp = shapeFileReader.readFileAndInitialize(boundaryPolygon);
            //works for only one polygon since we get one polygon
            MultiPolygon zurich = (MultiPolygon) ((SimpleFeatureImpl) zurich_shp.toArray()[0]).getAttribute(0);
            //todo for random generation since pop density only uses links of population within pop areas
            //todo make generic still for all - hub, density, etc.
        }


        //read in the input files
        String networkfile = cmd.getOptionStrict("networkFile");
        String populationfile = cmd.getOptionStrict("populationFile");
        int numberofVehicles = Integer.parseInt(cmd.getOptionStrict("fleetSize"));
        double operationStartTime = Double.parseDouble(cmd.getOptionStrict("operationStartTime")); //t0
        double operationEndTime = Double.parseDouble(cmd.getOptionStrict("operationEndTime"));    //t1
        int seats = Integer.parseInt(cmd.getOptionStrict("seats"));

        //read network and population in
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkfile);
        new PopulationReader(scenario).readFile(populationfile);
        Population population = scenario.getPopulation();

        String taxisFile = cmd.getOptionStrict("outputpath") + "/" + nameSuffix + "_" + numberofVehicles + "_" + seats + ".xml";

        CreateDrtVehicles createDrtVehicles = new CreateDrtVehicles(numberofVehicles, operationStartTime, operationEndTime, seats); //todo adjust to give one with random seed if option random seed given

        createDrtVehicles.createVehicles(network, population, taxisFile);

    }
}
