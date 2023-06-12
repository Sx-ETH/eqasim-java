package org.eqasim.switzerland.drt;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
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
                .allowOptions("output-path", "lastIteration", "flowCapacityFactor", "storageCapacityFactor", "drt_vehicles")
                .build();


        String config_path = cmd.getOptionStrict("config-path");


        SwissDrtConfigurator configurator = new SwissDrtConfigurator();

        Config config = ConfigUtils.loadConfig(config_path, configurator.getConfigGroups());
        configurator.configure(config);
        cmd.applyConfiguration(config);
        configurator.configureDrtTimeMetrics(config);

        String output_path = cmd.getOption("output-path").isPresent() ? cmd.getOption("output-path").get()
                : config.controler().getOutputDirectory();

        config.controler().setOutputDirectory(output_path);

        int lastIteration = cmd.getOption("lastIteration").isPresent() ? Integer.parseInt(cmd.getOption("lastIteration").get())
                : config.controler().getLastIteration();
        config.controler().setLastIteration(lastIteration);

        double flowCapacityFactor = cmd.getOption("flowCapacityFactor").isPresent() ? Double.parseDouble(cmd.getOption("flowCapacityFactor").get())
                : config.qsim().getFlowCapFactor();
        config.qsim().setFlowCapFactor(flowCapacityFactor);

        double storageCapacityFactor = cmd.getOption("storageCapacityFactor").isPresent() ? Double.parseDouble(cmd.getOption("storageCapacityFactor").get())
                : config.qsim().getStorageCapFactor();
        config.qsim().setStorageCapFactor(storageCapacityFactor);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        //Set bike run options
        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        configurator.configureScenario(scenario);
        configurator.adjustDrtScenario(scenario);
        AstraConfigurator.adjustScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);


        controller.addOverridingModule(new SwissModeChoiceModule(cmd));
        controller.addOverridingModule(new EqasimModeChoiceModule());

        // Configure controller for DRT adding dvrp and drt modules
        SwissDrtConfigurator.configureController(controller, cmd, config, scenario);

        String drt_vehicles = cmd.getOption("drt_vehicles").isPresent() ? cmd.getOption("drt_vehicles").get()
                : ((DrtConfigGroup) config.getModules().get("multiModeDrt").getParameterSets("drt").iterator().next()).getVehiclesFile();
        ((DrtConfigGroup) config.getModules().get("multiModeDrt").getParameterSets("drt").iterator().next()).setVehiclesFile(drt_vehicles);

        controller.run();
    }
}
