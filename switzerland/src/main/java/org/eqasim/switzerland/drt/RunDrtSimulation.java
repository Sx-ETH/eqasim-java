package org.eqasim.switzerland.drt;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDrtSimulation {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path").allowOptions("drt-variables-estimator",
                        "drtRejectionsPenaltyProvider", "use-am",
                        "", "", "", "",
                        "") //
                .allowPrefixes("mode-choice-parameter", "cost-parameter") //
                .allowOptions("output-path")
                .allowOptions("delayFactorFeedback", "waitTimeFeedback")
                .build();


        String config_path = cmd.getOptionStrict("config-path");


        SwissDrtConfigurator configurator = new SwissDrtConfigurator();

        Config config = ConfigUtils.loadConfig(config_path, configurator.getConfigGroups());
        configurator.configure(config);
        cmd.applyConfiguration(config);

        String output_path = cmd.getOption("output-path").isPresent() ? cmd.getOption("output-path").get()
                : config.controler().getOutputDirectory();

        config.controler().setOutputDirectory(output_path);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        SimulationParameter simulationParams = new SimulationParameter(config);

        //Set bike run options
        simulationParams.setDrtTravelTimeParams(cmd);

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        configurator.configureScenario(scenario);
        configurator.adjustDrtScenario(scenario);
        AstraConfigurator.adjustScenario(scenario);


        //does it adjust the scenario
        for (Person person : scenario.getPopulation().getPersons().values()) {
            person.getAttributes().getAttribute("householdIncome");
        }

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);


        controller.addOverridingModule(new SwissModeChoiceModule(cmd));
        controller.addOverridingModule(new EqasimModeChoiceModule());

        // Configure controller for DRT adding dvrp and drt modules
        SwissDrtConfigurator.configureController(controller, cmd, config, scenario);

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(SimulationParameter.class).toInstance(simulationParams);
            }
        });

        controller.run();
    }
}
