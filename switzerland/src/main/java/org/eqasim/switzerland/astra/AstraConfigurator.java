package org.eqasim.switzerland.astra;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.astra.estimators.AstraBikeUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraCarUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraPtUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraWalkUtilityEstimator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class AstraConfigurator extends SwitzerlandConfigurator {
    public AstraConfigurator() {
    }

    public static void configure(Config config) {
        // General MATSim
        config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
        config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

        // General eqasim
        EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        eqasimConfig.setAnalysisInterval(config.controler().getWriteEventsInterval());

        // Estimators
        eqasimConfig.setEstimator(TransportMode.car, AstraCarUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.pt, AstraPtUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.bike, AstraBikeUtilityEstimator.NAME);
        eqasimConfig.setEstimator(TransportMode.walk, AstraWalkUtilityEstimator.NAME);
    }

    public void adjustScenario(Scenario scenario) {
        new SwitzerlandConfigurator().adjustScenario(scenario);
    }

    public static void configureController(Controler controller, CommandLine commandLine) {
        controller.addOverridingModule(new AstraModule(commandLine));
    }


}
