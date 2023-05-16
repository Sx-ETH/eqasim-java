package org.eqasim.ile_de_france.drt;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.drt.mode_choice.IDFDrtModeAvailability;
import org.eqasim.ile_de_france.drt.mode_choice.cost.DrtCostModel;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtCostParameters;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtModeParameters;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.*;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty.*;
import org.eqasim.ile_de_france.feeder.FeederConstraint;
import org.eqasim.ile_de_france.feeder.FeederUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.modal.ModalAnnotationCreator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;

public class IDFDrtModule extends AbstractEqasimExtension {

	private final IDFDrtConfigGroup configGroup;


	private final CommandLine commandLine;


	public IDFDrtModule(CommandLine commandLine, IDFDrtConfigGroup configGroup) throws CommandLine.ConfigurationException {
		this.commandLine = commandLine;
		this.configGroup = configGroup;
	}

	@Override
	protected void installEqasimExtension() {
		// Configure mode availability
		bindModeAvailability(IDFDrtModeAvailability.NAME).toInstance(new IDFDrtModeAvailability(this.configGroup.isUsingFeeder()));
		// Configure choice alternative for DRT
		bindUtilityEstimator("drt").to(DrtUtilityEstimator.class);
		bindCostModel("drt").to(DrtCostModel.class);
		DrtRejectionPenaltyProviderConfigGroup drtRejectionPenaltyProviderConfigGroup = configGroup.getDrtRejectionPenaltyProviderConfig();
		if(drtRejectionPenaltyProviderConfigGroup != null && drtRejectionPenaltyProviderConfigGroup.getPenaltyProviderParams() instanceof DrtRejectionsLinearPenaltyProviderConfigGroup) {
			bind(DrtRejectionsLinearPenaltyProviderConfigGroup.class).toInstance((DrtRejectionsLinearPenaltyProviderConfigGroup) drtRejectionPenaltyProviderConfigGroup.getPenaltyProviderParams());
			bind(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
			bind(DrtRejectionPenaltyProvider.class).to(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
			addControlerListenerBinding().to(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
			bind(RejectionTracker.class).asEagerSingleton();
			addEventHandlerBinding().to(RejectionTracker.class).asEagerSingleton();
		} else {
			bind(DrtRejectionPenaltyProvider.class).to(NoRejectionsPenalty.class).asEagerSingleton();
		}
		bind(DrtPredictorInterface.class).to(DrtPredictor.class);

		if(configGroup.isUsingFeeder()) {
			bindUtilityEstimator("feeder").to(FeederUtilityEstimator.class);
			bindTripConstraintFactory(FeederConstraint.NAME).to(FeederConstraint.Factory.class);
		}

		// Define filter for trip analysis
		bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);

		// Override parameter bindings
		bind(ModeParameters.class).to(IDFDrtModeParameters.class);
		bind(IDFModeParameters.class).to(IDFDrtModeParameters.class);
		bind(IDFCostParameters.class).to(IDFDrtCostParameters.class);

		if(this.configGroup.getOverridePcu() != null) {
			VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("Feeder", VehicleType.class));
			vehicleType.setPcuEquivalents(this.configGroup.getOverridePcu());
			bind(VehicleType.class).annotatedWith(DvrpModes.mode("drt")).toInstance(vehicleType);
		}
	}

	@Provides
	@Singleton
	public DrtCostModel provideDrtCostModel(IDFDrtCostParameters parameters) {
		return new DrtCostModel(parameters);
	}

	@Provides
	@Singleton
	public IDFDrtCostParameters provideCostParameters(EqasimConfigGroup config) throws URISyntaxException {
		IDFDrtCostParameters parameters = IDFDrtCostParameters.buildDefault();
		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(ConfigGroup.getInputFileURL(getConfig().getContext(), config.getCostParametersPath()).toURI()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public IDFDrtModeParameters provideModeParameters(EqasimConfigGroup config) throws URISyntaxException {
		IDFDrtModeParameters parameters = IDFDrtModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(ConfigGroup.getInputFileURL(getConfig().getContext(), config.getModeParametersPath()).toURI()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Named("drt")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config, @DvrpMode("drt") VehicleType vehicleType) {
		return getCostModel(factory, config, "drt");
	}

}
