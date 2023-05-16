package org.matsim.contribs.discrete_mode_choice.modules;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.EventFiringDelegatingModel;
import org.matsim.contribs.discrete_mode_choice.model.EventFiringTourBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTourFilter;
import org.matsim.contribs.discrete_mode_choice.model.filters.CompositeTripFilter;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.DefaultModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.timing.TimeInterpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class EventFiringModelModule extends AbstractModule {

    public void install() {
        install(new ModeAvailabilityModule());
        install(new EstimatorModule());
        install(new TourFinderModule());
        install(new SelectorModule());
        install(new ConstraintModule());
        install(new FilterModule());
        install(new HomeFinderModule());

        bind(ModeChainGeneratorFactory.class).to(DefaultModeChainGenerator.Factory.class);
    }

    public enum ModelType {
        Trip, Tour
    }

    @Provides
    public DiscreteModeChoiceModel provideDiscreteModeChoiceModel(DiscreteModeChoiceConfigGroup dmcConfig,
                                                                  Provider<EventFiringTourBasedModel> tourBasedProvider, Provider<TripBasedModel> tripBasedProvider,
                                                                  EventsManager eventsManager) {
        switch (dmcConfig.getModelType()) {
            case Tour:
                return new EventFiringDelegatingModel(eventsManager, tourBasedProvider.get());
            case Trip:
                return new EventFiringDelegatingModel(eventsManager, tripBasedProvider.get());
            default:
                throw new IllegalStateException();
        }
    }

    @Provides
    public EventFiringTourBasedModel provideTourBasedModel(ModeAvailability modeAvailability, TourFilter tourFilter,
                                                TourEstimator tourEstimator, TourConstraintFactory tourConstraintFactory, TourFinder tourFinder,
                                                UtilitySelectorFactory selectorFactory, ModeChainGeneratorFactory modeChainGeneratorFactory,
                                                DiscreteModeChoiceConfigGroup dmcConfig, TimeInterpretation timeInterpreterFactory,
                                                EventsManager eventsManager) {
        return new EventFiringTourBasedModel(tourEstimator, modeAvailability, tourConstraintFactory, tourFinder, tourFilter,
                selectorFactory, modeChainGeneratorFactory, dmcConfig.getFallbackBehaviour(), timeInterpreterFactory, eventsManager);
    }

    @Provides
    public TripBasedModel provideTripBasedModel(TripEstimator estimator, TripFilter tripFilter,
                                                ModeAvailability modeAvailability, TripConstraintFactory constraintFactory,
                                                UtilitySelectorFactory selectorFactory, DiscreteModeChoiceConfigGroup dmcConfig,
                                                TimeInterpretation timeInterpreterFactory) {
        return new TripBasedModel(estimator, tripFilter, modeAvailability, constraintFactory, selectorFactory,
                dmcConfig.getFallbackBehaviour(), timeInterpreterFactory);
    }

    @Provides
    @Singleton
    public DefaultModeChainGenerator.Factory provideDefaultModeChainGeneratorFactory() {
        return new DefaultModeChainGenerator.Factory();
    }

    @Provides
    public TripFilter provideTripFilter(DiscreteModeChoiceConfigGroup dmcConfig,
                                        Map<String, Provider<TripFilter>> providers) {
        Collection<String> names = dmcConfig.getTripFilters();
        Collection<TripFilter> filters = new ArrayList<>(names.size());

        for (String name : names) {
            if (!providers.containsKey(name)) {
                throw new IllegalStateException(String.format("TripFilter '%s' does not exist.", name));
            } else {
                filters.add(providers.get(name).get());
            }
        }

        return new CompositeTripFilter(filters);
    }

    @Provides
    public TourFilter provideTourFilter(DiscreteModeChoiceConfigGroup dmcConfig,
                                        Map<String, Provider<TourFilter>> providers) {
        Collection<String> names = dmcConfig.getTourFilters();
        Collection<TourFilter> filters = new ArrayList<>(names.size());

        for (String name : names) {
            if (!providers.containsKey(name)) {
                throw new IllegalStateException(String.format("TourFilter '%s' does not exist.", name));
            } else {
                filters.add(providers.get(name).get());
            }
        }

        return new CompositeTourFilter(filters);
    }

    @Provides
    public TripListConverter provideTripListConverter() {
        return new TripListConverter();
    }
}
