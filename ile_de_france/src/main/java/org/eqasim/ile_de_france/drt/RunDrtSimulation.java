package org.eqasim.ile_de_france.drt;

import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.CbaUtils;
import org.eqasim.ile_de_france.drt.analysis.DvrpAnalsisModule;
import org.eqasim.ile_de_france.drt.mode_choice.DrtEpsilonModule;
import org.eqasim.ile_de_france.feeder.FeederModule;
import org.eqasim.ile_de_france.feeder.analysis.FeederAnalysisModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDrtSimulation {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path").allowOptions("drt-variables-estimator", "drtRejectionsPenaltyProvider", "use-am", "cba") //
                .allowPrefixes("mode-choice-parameter", "cost-parameter") //
                .build();

        boolean cba = cmd.hasOption("cba") && Boolean.parseBoolean(cmd.getOptionStrict("cba"));

        IDFDrtConfigurator configurator = new IDFDrtConfigurator();

        Config config;
        ConfigGroup[] configGroups = configurator.getConfigGroups();
        config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        if(cba) {
            CbaUtils.adaptConfig(config, true);
        }
        cmd.applyConfiguration(config);

        MultiModeDrtConfigGroup multiModeDrtConfig;

        if(config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME) == null) {
            throw new IllegalStateException("MultiModeDrt module should be specified in the configuration");
        }
        multiModeDrtConfig = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);

        for(DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
            drtConfigGroup.setNumberOfThreads(config.global().getNumberOfThreads());
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        ScenarioUtils.loadScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);

        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));

        { // Configure controller for DRT
            controller.addOverridingModule(new DvrpModule());
            controller.addOverridingModule(new MultiModeDrtModule());

            MultiModeDrtConfigGroup finalMultiModeDrtConfig = multiModeDrtConfig;
            Config finalConfig = config;
            controller.configureQSimComponents(components -> {
                DvrpQSimComponents.activateAllModes(finalMultiModeDrtConfig).configure(components);

                // Need to re-do this as now it is combined with DRT
                EqasimTransitQSimModule.configure(components, finalConfig);
            });
        }

        { // Add overrides for Corsica + DRT
            IDFDrtConfigGroup idfDrtConfigGroup = (IDFDrtConfigGroup) config.getModules().get(IDFDrtConfigGroup.GROUP_NAME);
            controller.addOverridingModule(new IDFDrtModule(cmd, idfDrtConfigGroup));
            controller.addOverridingModule(new DvrpAnalsisModule());
            if(idfDrtConfigGroup.isUsingFeeder()) {
                controller.addOverridingModule(new FeederModule(null, scenario.getTransitSchedule()));
                controller.addOverridingModule(new FeederAnalysisModule());
            }
        }
        {
            //Add support of Epsilon utility sstimators
            controller.addOverridingModule(new EpsilonModule());
            controller.addOverridingModule(new DrtEpsilonModule());
        }
        if(cba) {
            CbaUtils.adaptControler(controller);
        }
        controller.run();
    }
}
