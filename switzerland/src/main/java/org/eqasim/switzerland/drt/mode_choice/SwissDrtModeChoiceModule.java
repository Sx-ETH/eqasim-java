package org.eqasim.switzerland.drt.mode_choice;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.switzerland.drt.mode_choice.cost.DrtCostModel;
import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtCostParameters;
import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtModeParameters;
import org.eqasim.switzerland.drt.mode_choice.utilities.DrtPredictor;
import org.eqasim.switzerland.drt.mode_choice.utilities.DrtUtilityEstimator;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.matsim.core.config.CommandLine;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SwissDrtModeChoiceModule extends AbstractEqasimExtension {

    private final CommandLine commandLine;

    public SwissDrtModeChoiceModule(CommandLine commandLine) {
        this.commandLine = commandLine;
    }


    @Override
    protected void installEqasimExtension() {
        // Configure mode availability
        bindModeAvailability(SwissDrtModeAvailability.NAME).to(SwissDrtModeAvailability.class);

        // Configure choice alternative for DRT
        bindUtilityEstimator("drt").to(DrtUtilityEstimator.class);
        bindCostModel("drt").to(DrtCostModel.class);
        bind(DrtPredictor.class);


        // Override parameter bindings
        bind(SwissModeParameters.class).to(SwissDrtModeParameters.class);
        bind(SwissCostParameters.class).to(SwissDrtCostParameters.class);
    }

    @Provides
    @Singleton
    public SwissDrtModeParameters provideSwissDrtModeParameters(EqasimConfigGroup config)
            throws IOException, CommandLine.ConfigurationException {
        SwissDrtModeParameters parameters = SwissDrtModeParameters.buildASTRA2016();

        if (config.getModeParametersPath() != null) {
            ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
        }

        ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
        return parameters;
    }

    @Provides
    @Singleton
    public SwissDrtCostParameters provideDrtCostParameters(EqasimConfigGroup config) {
        SwissDrtCostParameters parameters = SwissDrtCostParameters.buildDefault();

        if (config.getCostParametersPath() != null) {
            ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
        }

        ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
        return parameters;
    }

    @Provides
    @Singleton
    public DrtCostModel provideDrtCostModel(SwissDrtCostParameters parameters) {
        return new DrtCostModel(parameters);
    }

    @Provides
    @Named("drt")
    public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
        return getCostModel(factory, config, "drt");
    }
}
