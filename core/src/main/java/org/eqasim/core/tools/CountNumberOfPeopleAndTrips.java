package org.eqasim.core.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Map;

public class CountNumberOfPeopleAndTrips {
    public static void main(String[] args) {

        String populationFile = args[0];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);


        Population population = scenario.getPopulation();
        new PopulationReader(scenario).readFile(populationFile);

        ScenarioUtils.loadScenario(scenario);


        System.out.println("Number of people: " + population.getPersons().size());
        // compute total number of trips
        int totalNumberOfTrips = 0;
        int i = 0;
        for (Map.Entry<Id<Person>, ? extends Person> personEntry : population.getPersons().entrySet()) {
            if (i % 10000 == 0) {
                System.out.println("Number of people processed: " + i);
            }
            Person person = personEntry.getValue();
            totalNumberOfTrips += TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements()).size();
            i += 1;
        }
        System.out.println("Total number of trips: " + totalNumberOfTrips);
    }
}

