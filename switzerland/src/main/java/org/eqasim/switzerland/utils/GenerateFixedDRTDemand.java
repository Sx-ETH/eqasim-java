package org.eqasim.switzerland.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GenerateFixedDRTDemand {

    public static void main(String[] args) throws IOException {

        String populationFile = args[0];
        String outputfile = args[1];
        int drtDemand = Integer.parseInt(args[2]);

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(populationFile);

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
                    System.out.println(distance);
                }
            }
        }
        System.out.println(candidateLegs.size() + " legs found");

        Collections.shuffle(candidateLegs, ThreadLocalRandom.current());

        for (int i = 0; i < drtDemand; i++) {
            Leg leg = candidateLegs.get(i);
            leg.setMode("drt");
        }

        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");

    }

}