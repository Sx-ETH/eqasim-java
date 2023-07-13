package org.eqasim.switzerland.drt;

import org.eqasim.core.components.drt.travel_times.TravelTimeConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.astra.AstraDrtConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
                .build();


        String config_path = cmd.getOptionStrict("config-path");


        AstraDrtConfigurator configurator = new AstraDrtConfigurator();

        Config config = ConfigUtils.loadConfig(config_path, configurator.getConfigGroups());
        configurator.configure(config);
        cmd.applyConfiguration(config);
        new TravelTimeConfigurator().configureDrtTimeMetrics(config);

        String output_path = cmd.getOption("output-path").isPresent() ? cmd.getOption("output-path").get()
                : config.controler().getOutputDirectory();

        config.controler().setOutputDirectory(output_path);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        //Set bike run options
        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        configurator.configureScenario(scenario);
        configurator.adjustDrtScenario(scenario);
        configurator.adjustLinkSpeed(config, scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);


        controller.addOverridingModule(new SwissModeChoiceModule(cmd));
        controller.addOverridingModule(new EqasimModeChoiceModule());

        // Configure controller for DRT adding dvrp and drt modules
        AstraDrtConfigurator.configureController(controller, cmd, config, scenario);

        controller.run();
    }
}
