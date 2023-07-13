package org.eqasim.switzerland.astra;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.astra.estimators.AstraBikeUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraCarUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraPtUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraWalkUtilityEstimator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.households.Household;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class AstraConfigurator extends SwitzerlandConfigurator {
    public AstraConfigurator() {
        this.configGroups.add(new AstraConfigGroup());
    }

    public static void configure(Config config) {
        // General MATSim
        config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
        config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

        // General eqasim
        EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        eqasimConfig.setAnalysisInterval(config.controler().getWritePlansInterval());

        // Estimators
        eqasimConfig.setEstimator(TransportMode.car, AstraCarUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.pt, AstraPtUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.bike, AstraBikeUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.walk, AstraWalkUtilityEstimator.NAME);

        DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
                .get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

        Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
        tripConstraints.add(InfiniteHeadwayConstraint.NAME);
        dmcConfig.setTripConstraints(tripConstraints);
    }

    public void adjustScenario(Scenario scenario) {
        new SwitzerlandConfigurator().adjustScenario(scenario);
        //include household attributes to swiss population
        for (Household household : scenario.getHouseholds().getHouseholds().values()) {
            for (Id<Person> memberId : household.getMemberIds()) {
                Person person = scenario.getPopulation().getPersons().get(memberId);

                if (person != null) {
                    person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
                }
            }
        }
        adjustBikeAvailability(scenario);
    }

    static private void adjustBikeAvailability(Scenario scenario) {
        Random random = new Random(scenario.getConfig().global().getRandomSeed());
        AstraConfigGroup astraConfig = AstraConfigGroup.get(scenario.getConfig());

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (!person.getId().toString().contains("freight")) {
                if (!person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
                    if (random.nextDouble() > astraConfig.getBikeAvailability()) {
                        person.getAttributes().putAttribute("bikeAvailability", "FOR_NONE");
                    }
                }
            }
        }
    }

    public static void configureController(Controler controller, CommandLine commandLine) {
        controller.addOverridingModule(new AstraModule(commandLine));
        controller.configureQSimComponents(configurator -> {
            EqasimTransitQSimModule.configure(configurator, controller.getConfig());
        });
    }

    public void adjustLinkSpeed(Config config, Scenario scenario) {
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

        for (Link link : scenario.getNetwork().getLinks().values()) {
            double maximumSpeed = link.getFreespeed();
            boolean isMajor = true;

            for (Link other : link.getToNode().getInLinks().values()) {
                if (other.getCapacity() >= link.getCapacity()) {
                    isMajor = false;
                }
            }

            if (!isMajor && link.getToNode().getInLinks().size() > 1) {
                double travelTime = link.getLength() / maximumSpeed;
                travelTime += eqasimConfig.getCrossingPenalty();
                link.setFreespeed(link.getLength() / travelTime);
            }
        }


    }


}
