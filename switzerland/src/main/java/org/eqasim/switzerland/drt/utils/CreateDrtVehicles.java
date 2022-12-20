package org.eqasim.switzerland.drt.utils;

/**
 * @author kaghog created on 06.12.2021
 * @project sampling-drt
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.CommandLine;
import org.matsim.core.gbl.MatsimRandom;

import java.io.IOException;
import java.util.*;


public class CreateDrtVehicles {

    /**
     * @param args
     */

    //vehicle params
    private int numberofVehicles;
    private double operationStartTime; //t0
    private double operationEndTime; //t1
    private int seats;
    private Random random;
    private Long randomSeed;
    public static final List<Link> linkList = new LinkedList<>();

    public CreateDrtVehicles(Long randomSeed, int numberofVehicles, double operationStartTime, double operationEndTime, int seats) {
        this.numberofVehicles = numberofVehicles;
        this.operationStartTime = operationStartTime;
        this.operationEndTime = operationEndTime;
        this.seats = seats;
        this.randomSeed = randomSeed;
        this.random = new Random(randomSeed);
    }

    //when random seed is not provided
    public CreateDrtVehicles(int numberofVehicles, double operationStartTime, double operationEndTime, int seats) {
        this.numberofVehicles = numberofVehicles;
        this.operationStartTime = operationStartTime;
        this.operationEndTime = operationEndTime;
        this.seats = seats;
        this.random = MatsimRandom.getLocalInstance();
    }

    public CreateDrtVehicles() {
    }

    public List<DvrpVehicleSpecification> RandomPlacementGenerator(Network network) {

        List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
        List<Id<Link>> allLinks = new ArrayList<>();
        allLinks.addAll(network.getLinks().keySet());

        //create vehicles
        for (int i = 0; i< numberofVehicles;i++){
            Link startLink;
            do {
                Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
                startLink =  network.getLinks().get(linkId);
            }
            while (!startLink.getAllowedModes().contains(TransportMode.car));
            //for multi-modal networks: Only links where cars can ride should be used.
            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt" + i, DvrpVehicle.class))
                    .startLinkId(startLink.getId())
                    .capacity(seats)
                    .serviceBeginTime(operationStartTime)
                    .serviceEndTime(operationEndTime)
                    .build());

        }

        return vehicles;

    }

    public List<DvrpVehicleSpecification> populationDensityGenerator(Map<Link, Double> cumulativeDensity) {

        //adapted from @sebhoerl av/generator/PopulationDensityGenerator

        List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

        //create vehicles
        Link startLink = null;
        //Link startLink = cumulativeDensity.entrySet().iterator().next().getKey(); //initializing with the first link
        for (int i = 0; i< numberofVehicles;i++){

            double r = random.nextDouble();
            do {
                for (Link link : linkList) {
                    if (r <= cumulativeDensity.get(link)) {
                        startLink = link;
                        break;
                    }
                }
            }
            while (!startLink.getAllowedModes().contains(TransportMode.car));
            //for multi-modal networks: Only links where cars can ride should be used.
            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt" + i, DvrpVehicle.class))
                    .startLinkId(startLink.getId())
                    .capacity(seats)
                    .serviceBeginTime(operationStartTime)
                    .serviceEndTime(operationEndTime)
                    .build());

        }

        return vehicles;

    }

    public Map<Link, Double> getPopulationDensity (Population population, Network network){
        Map<Link, Double> density = new HashMap<>();
        Map<Link, Double> cumulativeDensity = new HashMap<>();
        double totalActivities = 0.0;

        //get the cummulative sum for all links that home activities were performed

        for (Person person: population.getPersons().values()){
            Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);

            //ensure that the activities are not outside the service area
            if (act.getType().equals("outside")){
                continue;
            }
            Link link =  network.getLinks().get(act.getLinkId());

            if (density.containsKey(link)) {
                density.put(link, density.get(link) + 1.0);
            } else {
                density.put(link, 1.0);
            }

            if (!linkList.contains(link)){
                linkList.add(link);
            }
            totalActivities += 1.0;

        }
        //create cummulative frequency of links
        double cumsum = 0.0;

        for (Link l : linkList) {
            cumsum += density.get(l) / totalActivities;
            cumulativeDensity.put(l, cumsum);
        }

        return cumulativeDensity;
    }


    public String createVehicles(Network network, Population population, String taxisFile) throws CommandLine.ConfigurationException, IOException {
        List<DvrpVehicleSpecification> vehicles;

        //generate vehicles randomly ToDo: generate by hub or by population density
        //vehicles = RandomPlacementGenerator(network);
        vehicles = populationDensityGenerator(getPopulationDensity(population, network));

        new FleetWriter(vehicles.stream()).write(taxisFile);

        return taxisFile;
    }

}

