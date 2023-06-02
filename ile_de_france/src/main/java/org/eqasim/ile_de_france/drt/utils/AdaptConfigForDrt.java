package org.eqasim.ile_de_france.drt.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.drt.IDFDrtConfigGroup;
import org.eqasim.ile_de_france.drt.mode_choice.IDFDrtModeAvailability;
import org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionPenaltyProviderConfigGroup;
import org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionsLinearPenaltyProviderConfigGroup;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.misc.Time;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AdaptConfigForDrt {

    public void adapt(Config config, String vehiclesPath, String qsimEndtime) {
        IDFDrtConfigGroup idfDrtConfigGroup = new IDFDrtConfigGroup();
        DrtRejectionPenaltyProviderConfigGroup rejectionPenaltyProviderConfigGroup = new DrtRejectionPenaltyProviderConfigGroup();
        rejectionPenaltyProviderConfigGroup.addParameterSet(new DrtRejectionsLinearPenaltyProviderConfigGroup());
        idfDrtConfigGroup.addParameterSet(rejectionPenaltyProviderConfigGroup);
        config.addModule(idfDrtConfigGroup);


        DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
        config.addModule(dvrpConfig);

        MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
        config.addModule(multiModeDrtConfig);
        DrtConfigGroup drtConfig = new DrtConfigGroup();
        drtConfig.setMode("drt");
        drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
        drtConfig.setStopDuration(15.0);
        drtConfig.setMaxWaitTime(600.0);
        drtConfig.setMaxTravelTimeAlpha(1.5);
        drtConfig.setMaxTravelTimeBeta(300.0);


        drtConfig.setVehiclesFile(vehiclesPath);

        DrtInsertionSearchParams searchParams = new SelectiveInsertionSearchParams();
        drtConfig.addDrtInsertionSearchParams(searchParams);

        multiModeDrtConfig.addDrtConfig(drtConfig);
        DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

        // Additional requirements
        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setEndTime(Time.parseOptionalTime(qsimEndtime).seconds());
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        // Add DRT to the available modes
        dmcConfig.setModeAvailability(IDFDrtModeAvailability.NAME);

        // Add DRT to cached modes
        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("drt");
        dmcConfig.setCachedModes(cachedModes);

        // Set up choice model
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
        eqasimConfig.setCostModel("drt", "drt");
        eqasimConfig.setEstimator("drt", "drt");

        // Set analysis interval
        eqasimConfig.setAnalysisInterval(1);

        PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt");
        config.planCalcScore().addModeParams(modeParams);
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-config-path", "output-config-path", "vehicles-path")
                .allowOptions("qsim-endtime")
                .build();
        String inputConfigPath = cmd.getOptionStrict("input-config-path");
        String outputConfigPath = cmd.getOptionStrict("output-config-path");
        String vehiclesPath = cmd.getOptionStrict("vehicles-path");
        String qsimEndtime = cmd.getOption("qsim-endtime").orElse("30:00:00");

        Path path = Path.of(outputConfigPath).getParent().toAbsolutePath().relativize(Path.of(vehiclesPath).toAbsolutePath());

        IDFConfigurator configurator = new IDFConfigurator();
        Config config = ConfigUtils.loadConfig(inputConfigPath, configurator.getConfigGroups());

        new AdaptConfigForDrt().adapt(config, path.toString(), qsimEndtime);

        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}
