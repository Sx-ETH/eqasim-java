package org.matsim.contribs.discrete_mode_choice.modules;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.SelectedToursRecorder;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.UtilitiesRecorder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.utils.ModeChoiceInTheLoopChecker;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.timing.TimeInterpretationModule;

import java.util.List;


public class EventFiringDiscreteModeChoiceModule extends AbstractModule {

    public static final String LAST_ITERATION_TOUR_FILTER = "lastIterationTourFilter";

    public static class LastIterationTourFilter implements TourFilter {

        private final ReplanningContext replanningContext;
        private final Config config;

        @Inject
        public LastIterationTourFilter(ReplanningContext replanningContext, Config config) {
            this.replanningContext = replanningContext;
            this.config = config;
        }

        @Override
        public boolean filter(Person person, List<DiscreteModeChoiceTrip> tour) {
            return config.controler().getLastIteration() != replanningContext.getIteration();
        }
    }

    public static final String STRATEGY_NAME = "DiscreteModeChoice";

    @Inject
    private DiscreteModeChoiceConfigGroup dmcConfig;

    @Inject
    private Config config;

    @Override
    public void install() {
        addPlanStrategyBinding(STRATEGY_NAME).toProvider(DiscreteModeChoiceStrategyProvider.class);

        if (getConfig().strategy().getPlanSelectorForRemoval().equals(NonSelectedPlanSelector.NAME)) {
            bindPlanSelectorForRemoval().to(NonSelectedPlanSelector.class);
        }

        if (dmcConfig.getEnforceSinglePlan()) {
            addControlerListenerBinding().to(ModeChoiceInTheLoopChecker.class);
        }

        install(new EventFiringModelModule());
        install(new TimeInterpretationModule());
        if(!dmcConfig.getWriteUtilities().equals(DiscreteModeChoiceConfigGroup.WriteUtilities.NONE)) {
            MapBinder.newMapBinder(binder(), String.class, TourFilter.class).addBinding(LAST_ITERATION_TOUR_FILTER).to(LastIterationTourFilter.class);
            if(dmcConfig.getWriteUtilities().equals(DiscreteModeChoiceConfigGroup.WriteUtilities.ALL)) {
                throw new IllegalStateException("Logging the utilities on all iterations is not supported yet");
            }
            dmcConfig.getTourFilters().add(LAST_ITERATION_TOUR_FILTER);
            for(StrategyConfigGroup.StrategySettings settings: config.strategy().getStrategySettings()) {
                if(!settings.getStrategyName().equals("DiscreteModeChoice")) {
                    settings.setDisableAfter(config.controler().getLastIteration() - 1);
                }
            }
            addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
                @Inject MatsimServices matsimServices;

                @Inject EventsManager eventsManager;

                @Inject
                Population population;

                @Override
                public ControlerListener get() {
                    return new UtilitiesRecorder(matsimServices, eventsManager, population, dmcConfig.getWriteUtilities().equals(DiscreteModeChoiceConfigGroup.WriteUtilities.LAST));
                }
            }).asEagerSingleton();
        }
        if(dmcConfig.getWriteTourChoices()) {
            addControlerListenerBinding().to(SelectedToursRecorder.class).asEagerSingleton();
        }
    }
}


