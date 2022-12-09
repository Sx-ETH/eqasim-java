package org.eqasim.switzerland.drt;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.drt.TravelTimes.wait_time.SwissDrtWaitTimeModule;
import org.eqasim.switzerland.drt.mode_choice.SwissDrtModeAvailability;
import org.eqasim.switzerland.drt.mode_choice.SwissDrtModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.zonal.DrtModeZonalSystemModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

import java.util.HashSet;
import java.util.Set;

public class SwissDrtConfigurator extends SwitzerlandConfigurator {
    public SwissDrtConfigurator() {

    }

    @Override
    public ConfigGroup[] getConfigGroups() {
        return new ConfigGroup[] { //
                new SwissRailRaptorConfigGroup(), //
                new EqasimConfigGroup(), //
                new DiscreteModeChoiceConfigGroup(), //
                new CalibrationConfigGroup(), //
                new DvrpConfigGroup(), //
                new MultiModeDrtConfigGroup()

        };
    }

    public static void configure(Config config) {
        // General MATSim
        config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
        config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

        // Set up drt modeparams for matsim scoring
        PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt");
        config.planCalcScore().addModeParams(modeParams);


        // General eqasim

        EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        eqasimConfig.setAnalysisInterval(config.controler().getWriteEventsInterval());

        // Add Drt Estimators
        eqasimConfig.setCostModel("drt", "drt");
        eqasimConfig.setEstimator("drt", "drt");

        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        //Add mode availability that includes drt
        dmcConfig.setModeAvailability(SwissDrtModeAvailability.NAME);

        // Add DRT to cached modes
        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("drt");
        dmcConfig.setCachedModes(cachedModes);

        // Additional DRT requirements
        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

        if(config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME) == null) {
            throw new IllegalStateException("MultiModeDrt module should be specified in the configuration");
        }

        MultiModeDrtConfigGroup multiModeDrtConfig;
        multiModeDrtConfig = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);

        for(DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
            drtConfigGroup.setNumberOfThreads(config.global().getNumberOfThreads());
        }

        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(), config.plansCalcRoute());

    }

    public static void configureDrt(Config config){
        MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
        DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
        drtConfig.setMode("drt");
        drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
        drtConfig.setStopDuration(45.0);
        drtConfig.setMaxWaitTime(600.0);
        drtConfig.setMaxTravelTimeAlpha(1.5);
        drtConfig.setMaxTravelTimeBeta(300.0);
        drtConfig.setVehiclesFile("");

        //consider drt zones generation for the average wait time
        DrtZonalSystemParams zoneParams = drtConfig.getZonalSystemParams().orElseThrow();
        zoneParams.setCellSize(500.0);
        zoneParams.setZonesGeneration(DrtZonalSystemParams.ZoneGeneration.GridFromNetwork);

        DrtInsertionSearchParams searchParams = new ExtensiveInsertionSearchParams(); //new SelectiveInsertionSearchParams();
        drtConfig.addDrtInsertionSearchParams(searchParams);

        multiModeDrtConfig.addDrtConfig(drtConfig);

    }

    public static void adjustDrtScenario(Scenario scenario) {
        //include household attributes to swiss population
        new SwissDrtConfigurator().adjustScenario(scenario);

        //Add drt route factory
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
                new DrtRouteFactory());

    }


    public static void configureController(Controler controller, CommandLine cmd, Config config, Scenario scenario) {
        controller.addOverridingModule(new DvrpModule());
        controller.addOverridingModule(new MultiModeDrtModule());
        controller.configureQSimComponents(components -> {
            DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)).configure(components);
        });

        controller.addOverridingModule(new SwissDrtModeChoiceModule(cmd));

        //consider drt zones generation
        controller.addOverridingModule(new DrtModeZonalSystemModule(DrtConfigGroup.getSingleModeDrtConfig(config)));
        controller.addOverridingModule(new SwissDrtWaitTimeModule(DrtConfigGroup.getSingleModeDrtConfig(config), scenario));

        /*if (!config.qsim().getVehiclesSource().name().equals("defaultVehicle")) {
            controller.addOverridingModule(new AbstractModule() {

                @Override
                public void install() {
                    bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE))
                            .toInstance(
                                    scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.drt, VehicleType.class)));
                }
            });}
*/

    }

}
