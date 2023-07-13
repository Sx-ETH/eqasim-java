package org.eqasim.switzerland.astra;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.astra.estimators.AstraBikeUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraCarUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraPtUtilityEstimator;
import org.eqasim.switzerland.astra.estimators.AstraWalkUtilityEstimator;
import org.eqasim.switzerland.astra.predictors.*;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.core.config.CommandLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.File;
import java.io.IOException;

public class AstraModule extends AbstractEqasimExtension {
    private final CommandLine commandLine;


    public AstraModule(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    protected void installEqasimExtension() {
        bindUtilityEstimator(AstraCarUtilityEstimator.NAME).to(AstraCarUtilityEstimator.class);
        bindUtilityEstimator(AstraPtUtilityEstimator.NAME).to(AstraPtUtilityEstimator.class);
        bindUtilityEstimator(AstraBikeUtilityEstimator.NAME).to(AstraBikeUtilityEstimator.class);
        bindUtilityEstimator(AstraWalkUtilityEstimator.NAME).to(AstraWalkUtilityEstimator.class);

        bind(AstraPersonPredictor.class);
        bind(AstraTripPredictor.class);
        bind(AstraPtPredictor.class);
        bind(AstraBikePredictor.class);
        bind(AstraWalkPredictor.class);

        // Override parameter bindings
        bind(SwissModeParameters.class).to(AstraModeParameters.class);

        bindTripConstraintFactory(InfiniteHeadwayConstraint.NAME).to(InfiniteHeadwayConstraint.Factory.class);
    }

    @Provides
    @Singleton
    public AstraModeParameters provideAstraModeParameters(EqasimConfigGroup config)
            throws IOException, CommandLine.ConfigurationException {
        AstraModeParameters parameters = AstraModeParameters.buildFrom6Feb2020();

        if (config.getModeParametersPath() != null) {
            ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
        }

        ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
        return parameters;
    }

    @Provides
    @Singleton
    public OVGKCalculator provideOVGKCalculator(TransitSchedule transitSchedule) {
        return new OVGKCalculator(transitSchedule);
    }

}
