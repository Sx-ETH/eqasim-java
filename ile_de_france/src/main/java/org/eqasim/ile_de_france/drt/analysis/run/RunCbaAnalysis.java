package org.eqasim.ile_de_france.drt.analysis.run;

import com.google.inject.Injector;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.CbaUtils;
import org.eqasim.ile_de_france.drt.IDFDrtConfigGroup;
import org.eqasim.ile_de_france.drt.IDFDrtConfigurator;
import org.eqasim.ile_de_france.drt.IDFDrtModule;
import org.eqasim.ile_de_france.drt.analysis.DvrpAnalsisModule;
import org.eqasim.ile_de_france.feeder.FeederModule;
import org.eqasim.ile_de_france.feeder.analysis.FeederAnalysisModule;
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
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ExplodedConfigModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCbaAnalysis {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "plans-path", "output-path")
                .allowOptions("events-path")//
                .build();

        IDFDrtConfigurator configurator = new IDFDrtConfigurator();

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

        MultiModeDrtConfigGroup multiModeDrtConfig = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
        for(DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
            drtConfigGroup.setVehiclesFile("drt_vehicles.xml.gz");
        }

        CbaUtils.adaptConfig(config, true);


        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        ScenarioUtils.loadScenario(scenario);

        /*
        IDFDrtConfigGroup idfDrtConfigGroup = (IDFDrtConfigGroup) config.getModules().get(IDFDrtConfigGroup.GROUP_NAME);


        Injector injector = new InjectorBuilder(scenario)
                .addOverridingModules(configurator.getModules())
                .addOverridingModule(new EqasimAnalysisModule())
                .addOverridingModule(new EqasimModeChoiceModule())
                .addOverridingModule(new IDFModeChoiceModule(cmd))
                .addOverridingModule(new DvrpModule())
                .addOverridingModule(new MultiModeDrtModule())
                .addOverridingModule(new CbaModule())
                .addOverridingModule(new IDFDrtModule(cmd, idfDrtConfigGroup))
                .addOverridingModule(new FeederModule(null, scenario.getTransitSchedule()))
                .addOverridingModule(new FeederAnalysisModule())
                .addOverridingModule(new NewControlerModule() )
                .addOverridingModule(new ControlerDefaultCoreListenersModule())
                .build();

        EventsManager eventsManager = injector.getInstance(EventsManager.class);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
        eventsReader.readFile(cmd.getOptionStrict("events-path"));*/


        Controler controller = new Controler(scenario);
        configurator.configureController(controller);

        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));
        CbaUtils.adaptControler(controller);

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

        controller.run();

    }
}
