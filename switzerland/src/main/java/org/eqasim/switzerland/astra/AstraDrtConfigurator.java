package org.eqasim.switzerland.astra;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.DrtTravelTimeModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.switzerland.astra.estimators.AstraDrtUtilityEstimator;
import org.eqasim.switzerland.drt.mode_choice.cost.DrtCostModel;
import org.eqasim.switzerland.drt.mode_choice.cost.drt_cost_calculator.AstraDrtCostModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.analysis.zonal.DrtModeZonalSystemModule;
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
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AstraDrtConfigurator extends AstraConfigurator {
    public AstraDrtConfigurator() {
        this.configGroups.addAll(Arrays.asList( //
                new DvrpConfigGroup(), //
                new MultiModeDrtConfigGroup(), //
                new DrtModeChoiceConfigGroup()
        ));
    }


    public static void configure(Config config, CommandLine cmd) {
        AstraConfigurator.configure(config);

        // Set up drt modeparams for matsim scoring
        PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt");
        config.planCalcScore().addModeParams(modeParams);

        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
        // Add Drt Estimators
        eqasimConfig.setCostModel("drt", DrtCostModel.NAME);
        eqasimConfig.setEstimator("drt", AstraDrtUtilityEstimator.NAME); //todo configure to use astra or not.

        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        //Add mode availability that includes drt
        dmcConfig.setModeAvailability(AstraDrtModeAvailability.NAME);

        // Add DRT to cached modes
        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("drt");
        dmcConfig.setCachedModes(cachedModes);

        // Additional DRT requirements
        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

        if (config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME) == null) {
            throw new IllegalStateException("MultiModeDrt module should be specified in the configuration");
        }

        MultiModeDrtConfigGroup multiModeDrtConfig;
        multiModeDrtConfig = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);

        for (DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
            //drtConfigGroup.setNumberOfThreads(config.global().getNumberOfThreads());

            if (cmd.getOption("drt-vehicle-file").isPresent()) {
                drtConfigGroup.setVehiclesFile(cmd.getOption("drt-vehicle-file").get());
            }
        }

        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(), config.plansCalcRoute());

    }

    public void adjustDrtScenario(Scenario scenario) {
        // add bike and sp region and household attributes
        this.adjustScenario(scenario);

        //Add drt route factory
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
                new DrtRouteFactory());

        //correct output_plans file of previous run if used
        if (((ControlerConfigGroup) scenario.getConfig().getModules().get("controler")).getFirstIteration() > 0) {
            for (final Person person : scenario.getPopulation().getPersons().values()) {
                Plan plantoremove = null;
                for (final Plan plan : person.getPlans()) {
                    if (person.getSelectedPlan() != plan) {
                        plantoremove = plan;
                        continue;
                    }
                    for (final TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
                        for (final PlanElement pe : trip.getTripElements()) {
                            if (pe instanceof Leg) {
                                Leg leg = ((Leg) pe);

                                if (leg.getMode().equals("walk") & pe.getAttributes().getAttribute("routingMode").equals("drt")) {
                                    //For iteration run continuation, drt walk trips do have a
                                    // General RouteImpl routing type and triggers an error
                                    leg.setRoute(null);
                                    pe.getAttributes().putAttribute("routingMode", "drt");
                                }
                            }
                        }
                    }

                }

                person.getPlans().remove(plantoremove);
            }
        }
    }

    public static void configureController(Controler controller, CommandLine cmd, Config config, Scenario scenario) {
        AstraConfigurator.configureController(controller, cmd);
        controller.addOverridingModule(new DvrpModule());
        controller.addOverridingModule(new MultiModeDrtModule());
        Config finalConfig = config;
        controller.configureQSimComponents(components -> {
            DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)).configure(components);
            // Need to re-do this as now it is combined with DRT (copied from IdF)
            EqasimTransitQSimModule.configure(components, finalConfig);
        });

        controller.addOverridingModule(new AstraDrtModule(cmd));
        //controller.addOverridingModule(new DrtRejectionsPenaltyModule((DrtRejectionPenaltyProviderConfigGroup) config.getModules().get(DrtRejectionPenaltyProviderConfigGroup.SET_NAME)));

        //consider drt zones generation
        controller.addOverridingModule(new DrtModeZonalSystemModule(DrtConfigGroup.getSingleModeDrtConfig(config)));
        controller.addOverridingModule(new DrtTravelTimeModule(DrtConfigGroup.getSingleModeDrtConfig(config), scenario,
                (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME)));

        //add cost calculator module
        controller.addOverridingModule(new AstraDrtCostModule(DrtConfigGroup.getSingleModeDrtConfig(config)));

        //ToDo for multiple operators
        /*for (DrtConfigGroup singleDrtConfigGroup: multiModeConfig.getModalElements()){

            //controller.addOverridingModule(new DrtModeZonalSystemModule(singleDrtConfigGroup));
            //break;
//            controller.addOverridingModule(new SwissDrtTravelTimeModule(singleDrtConfigGroup, scenario,
//                    (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME)));
        }*/
    }
}
