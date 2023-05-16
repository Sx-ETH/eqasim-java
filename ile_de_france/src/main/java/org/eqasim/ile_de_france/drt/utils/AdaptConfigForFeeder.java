package org.eqasim.ile_de_france.drt.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.drt.IDFDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import java.nio.file.Path;

public class AdaptConfigForFeeder extends AdaptConfigForDrt{
    public void adapt(Config config, String vehiclesPath, String qsimEndtime) {
        super.adapt(config, vehiclesPath, qsimEndtime);
        IDFDrtConfigGroup idfDrtConfigGroup = (IDFDrtConfigGroup) config.getModules().get(IDFDrtConfigGroup.GROUP_NAME);
        idfDrtConfigGroup.setUsingFeeder(true);
        DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
        discreteModeChoiceConfigGroup.getTripConstraints().add("FeederConstraint");
        discreteModeChoiceConfigGroup.getTourFilters().remove("OutsideFilter");
        discreteModeChoiceConfigGroup.setFallbackBehaviour(DiscreteModeChoiceModel.FallbackBehaviour.INITIAL_CHOICE);
        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        eqasimConfigGroup.setEstimator("feeder", "feeder");
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = (PlanCalcScoreConfigGroup) config.getModules().get(PlanCalcScoreConfigGroup.GROUP_NAME);
        PlanCalcScoreConfigGroup.ActivityParams feederActivityParams = new PlanCalcScoreConfigGroup.ActivityParams("feeder interaction");
        feederActivityParams.setScoringThisActivityAtAll(false);
        planCalcScoreConfigGroup.getScoringParameters(null).addActivityParams(feederActivityParams);
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

        new AdaptConfigForFeeder().adapt(config, path.toString(), qsimEndtime);
        cmd.applyConfiguration(config);
        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}
