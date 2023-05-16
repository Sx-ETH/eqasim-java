package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.cba.CbaConfigGroup;
import org.matsim.contrib.cba.CbaModule;
import org.matsim.contrib.cba.analyzers.agentsAnalysis.AgentsAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.drtAnalysis.DrtAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.genericAnalysis.GenericAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.privateVehiclesAnalysis.PrivateVehiclesAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.ptAnalysis.PtAnalyzerConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCbaAnalysis {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "plans-path", "output-path")
                .build();



        IDFConfigurator configurator = new IDFConfigurator();

        Config config;
        ConfigGroup[] configGroups = configurator.getConfigGroups();
        config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        cmd.applyConfiguration(config);

        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(cmd.getOptionStrict("output-path"));
        config.plans().setInputFile(cmd.getOptionStrict("plans-path"));
        config.network().setInputFile("output_network.xml.gz");
        config.facilities().setInputFile("output_facilities.xml.gz");
        config.households().setInputFile("output_households.xml.gz");
        config.transit().setTransitScheduleFile("output_transitSchedule.xml.gz");
        config.transit().setVehiclesFile("output_transitVehicles.xml.gz");


        CbaUtils.adaptConfig(config, false);


        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        ScenarioUtils.loadScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);

        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));

        CbaUtils.adaptControler(controller);

        controller.run();

    }
}
