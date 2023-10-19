package org.eqasim.switzerland.astra;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.switzerland.astra.estimators.AstraDrtUtilityEstimator;
import org.eqasim.switzerland.astra.predictors.AstraDrtPredictor;
import org.eqasim.switzerland.drt.DrtPersonAnalysisFilter;
import org.eqasim.switzerland.drt.mode_choice.DrtDistanceConstraint;
import org.eqasim.switzerland.drt.mode_choice.DrtWaitTimeConstraint;
import org.eqasim.switzerland.drt.mode_choice.DrtWalkConstraint;
import org.eqasim.switzerland.drt.mode_choice.cost.DrtCostModel;
import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtCostParameters;
import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtAnalysisControlerListener;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtVehicleDistanceStats;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;

import java.io.File;
import java.util.Map;

public class AstraDrtModule extends AbstractEqasimExtension {
    private final CommandLine commandLine;


    public AstraDrtModule(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    protected void installEqasimExtension() {
        // Configure mode availability
        bind(SwissModeAvailability.class);
        bindModeAvailability(AstraDrtModeAvailability.NAME).to(AstraDrtModeAvailability.class);

        // Configure estimator for DRT
        bindUtilityEstimator(AstraDrtUtilityEstimator.NAME).to(AstraDrtUtilityEstimator.class);
        bindCostModel(DrtCostModel.NAME).to(DrtCostModel.class);
        bind(AstraDrtPredictor.class);

        bind(SwissCostParameters.class).to(SwissDrtCostParameters.class);

        //trip constraints
        bindTripConstraintFactory(DrtDistanceConstraint.NAME).to(DrtDistanceConstraint.Factory.class);
        bindTripConstraintFactory(DrtWaitTimeConstraint.NAME).to(DrtWaitTimeConstraint.Factory.class);
        bindTripConstraintFactory(DrtWalkConstraint.NAME).to(DrtWalkConstraint.Factory.class);

        // Person filter for eqasim analysis
        bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);


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
    public CostModel provideDrtCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
        return getCostModel(factory, config, "drt");
    }

    @Provides
    @Singleton
    public DrtDistanceConstraint.Factory provideShapeFileConstraintFactory(Network network) {
        return new DrtDistanceConstraint.Factory(network);
    }

    @Provides
    public AstraDrtModeAvailability provideAstraModeAvailability(SwissModeAvailability delegate) {
        return new AstraDrtModeAvailability(delegate);
    }
}
