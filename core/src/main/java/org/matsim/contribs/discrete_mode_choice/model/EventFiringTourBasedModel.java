package org.matsim.contribs.discrete_mode_choice.model;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.*;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.*;
import java.util.stream.Collectors;

public class EventFiringTourBasedModel implements DiscreteModeChoiceModel {
    final private static Logger logger = Logger.getLogger(TourBasedModel.class);

    final private TourFinder tourFinder;
    final private TourFilter tourFilter;
    final private TourEstimator estimator;
    final private ModeAvailability modeAvailability;
    final private TourConstraintFactory constraintFactory;
    final private UtilitySelectorFactory selectorFactory;
    final private ModeChainGeneratorFactory modeChainGeneratorFactory;
    final private FallbackBehaviour fallbackBehaviour;
    final private TimeInterpretation timeInterpreterFactory;
    final private EventsManager eventsManager;

    public EventFiringTourBasedModel(TourEstimator estimator, ModeAvailability modeAvailability,
                                     TourConstraintFactory constraintFactory, TourFinder tourFinder, TourFilter tourFilter,
                                     UtilitySelectorFactory selectorFactory, ModeChainGeneratorFactory modeChainGeneratorFactory,
                                     FallbackBehaviour fallbackBehaviour, TimeInterpretation timeInterpreterFactory,
                                     EventsManager eventsManager) {
        this.estimator = estimator;
        this.modeAvailability = modeAvailability;
        this.constraintFactory = constraintFactory;
        this.tourFinder = tourFinder;
        this.tourFilter = tourFilter;
        this.selectorFactory = selectorFactory;
        this.modeChainGeneratorFactory = modeChainGeneratorFactory;
        this.fallbackBehaviour = fallbackBehaviour;
        this.timeInterpreterFactory = timeInterpreterFactory;
        this.eventsManager = eventsManager;
    }

    @Override
    public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random)
            throws NoFeasibleChoiceException {
        List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));
        TourConstraint constraint = constraintFactory.createConstraint(person, trips, modes);

        List<TourCandidate> tourCandidates = new LinkedList<>();
        List<List<String>> tourCandidateModes = new LinkedList<>();

        int tripIndex = 1;
        TimeTracker time = new TimeTracker(timeInterpreterFactory);
        List<List<DiscreteModeChoiceTrip>> tours = tourFinder.findTours(trips);
        for (List<DiscreteModeChoiceTrip> tourTrips : tours) {
            time.addActivity(tourTrips.get(0).getOriginActivity());

            // We pass the departure time through the first origin activity
            tourTrips.get(0).setDepartureTime(time.getTime().seconds());

            TourCandidate finalTourCandidate = null;
            List<TourCandidate> utilityCandidates = new ArrayList<>();
            List<List<String>> tourModesExcludedBeforeEstimation = new ArrayList<>();
            List<TourCandidate> candidatesExcludedAfterEstimation = new ArrayList<>();
            if (tourFilter.filter(person, tourTrips)) {
                ModeChainGenerator generator = modeChainGeneratorFactory.createModeChainGenerator(modes, person,
                        tourTrips);
                UtilitySelector selector = selectorFactory.createUtilitySelector();

                while (generator.hasNext()) {
                    List<String> tourModes = generator.next();

                    if (!constraint.validateBeforeEstimation(tourTrips, tourModes, tourCandidateModes)) {
                        tourModesExcludedBeforeEstimation.add(tourModes);
                        continue;
                    }

                    TourCandidate candidate = estimator.estimateTour(person, tourModes, tourTrips, tourCandidates);

                    if (!Double.isFinite(candidate.getUtility())) {
                        logger.warn(buildIllegalUtilityMessage(tripIndex, person));
                        continue;
                    }

                    if (!constraint.validateAfterEstimation(tourTrips, candidate, tourCandidates)) {
                        candidatesExcludedAfterEstimation.add(candidate);
                        continue;
                    }
                    utilityCandidates.add(candidate);
                }
                boolean removed;
                do {
                    removed = false;
                    Set<Integer> nonRequiredFeederIndexes = new HashSet<>();
                    for(TourCandidate tourCandidate: utilityCandidates) {
                        for(int i=0; i<tourCandidate.getTripCandidates().size(); i++) {
                            TripCandidate tripCandidate = tourCandidate.getTripCandidates().get(i);
                            if(tripCandidate.getMode().equals("car") || tripCandidate.getMode().equals("pt")) {
                                nonRequiredFeederIndexes.add(i);
                            }
                        }
                    }
                    List<TourCandidate> newUtilityCandidates = new ArrayList<>();
                    for(TourCandidate tourCandidate: utilityCandidates) {
                        boolean goodToAdd = true;
                        for(int i=0; i<tourCandidate.getTripCandidates().size(); i++) {
                            if(tourCandidate.getTripCandidates().get(i).getMode().equals("feeder") && !nonRequiredFeederIndexes.contains(i)){
                                removed = true;
                                goodToAdd = false;
                            }
                        }
                        if(goodToAdd) {
                            newUtilityCandidates.add(tourCandidate);
                        }
                    }
                    utilityCandidates = newUtilityCandidates;
                }while(removed);
                for(TourCandidate tourCandidate: utilityCandidates){
                    selector.addCandidate(tourCandidate);
                }
                Optional<UtilityCandidate> selectedCandidate = selector.select(random);

                if (!selectedCandidate.isPresent()) {
                    switch (fallbackBehaviour) {
                        case INITIAL_CHOICE:
                            //logger.warn(buildFallbackMessage(tripIndex, person, "Setting tour modes back to initial choice."));
                            selectedCandidate = Optional.of(createFallbackCandidate(person, tourTrips, tourCandidates));
                            this.eventsManager.processEvent(new TourSelectorEvent(-1, person, utilityCandidates, (TourCandidate) selectedCandidate.get(), tourModesExcludedBeforeEstimation, candidatesExcludedAfterEstimation));
                            break;
                        case IGNORE_AGENT:
                            return handleIgnoreAgent(tripIndex, person, tourTrips);
                        case EXCEPTION:
                            throw new NoFeasibleChoiceException(buildFallbackMessage(tripIndex, person, ""));
                    }
                } else {
                    this.eventsManager.processEvent(new TourSelectorEvent(-1, person, utilityCandidates, (TourCandidate) selectedCandidate.get(), tourModesExcludedBeforeEstimation, candidatesExcludedAfterEstimation, "initial_choice"));
                }

                finalTourCandidate = (TourCandidate) selectedCandidate.get();
            } else {
                finalTourCandidate = createFallbackCandidate(person, tourTrips, tourCandidates);
                this.eventsManager.processEvent(new TourSelectorEvent(-1, person, utilityCandidates, finalTourCandidate, tourModesExcludedBeforeEstimation, candidatesExcludedAfterEstimation, "filtered_out"));
            }

            tourCandidates.add(finalTourCandidate);
            tourCandidateModes.add(
                    finalTourCandidate.getTripCandidates().stream().map(c -> c.getMode()).collect(Collectors.toList()));

            tripIndex += tourTrips.size();

            for (int i = 0; i < tourTrips.size(); i++) {
                if (i > 0) { // Our time object is already at the end of the first activity
                    time.addActivity(tourTrips.get(i).getOriginActivity());
                }

                time.addDuration(finalTourCandidate.getTripCandidates().get(i).getDuration());
            }
        }
        if(tours.size() == 0) {
            this.eventsManager.processEvent(new NoTourFoundForPersonEvent(-1, person));
        }
        return createTripCandidates(tourCandidates);
    }

    private TourCandidate createFallbackCandidate(Person person, List<DiscreteModeChoiceTrip> tourTrips,
                                                  List<TourCandidate> tourCandidates) {
        List<String> initialModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                .collect(Collectors.toList());
        return estimator.estimateTour(person, initialModes, tourTrips, tourCandidates);
    }

    private List<TripCandidate> createTripCandidates(List<TourCandidate> tourCandidates) {
        return tourCandidates.stream().map(TourCandidate::getTripCandidates).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<TripCandidate> handleIgnoreAgent(int tripIndex, Person person, List<DiscreteModeChoiceTrip> trips) {
        List<TourCandidate> tourCandidates = new LinkedList<>();

        for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
            List<String> tourModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                    .collect(Collectors.toList());
            tourCandidates.add(estimator.estimateTour(person, tourModes, tourTrips, tourCandidates));
        }

        logger.warn(buildFallbackMessage(tripIndex, person, "Setting whole plan back to initial modes."));
        return createTripCandidates(tourCandidates);
    }

    private String buildFallbackMessage(int tripIndex, Person person, String appendix) {
        return String.format("No feasible mode choice candidate for tour starting at trip %d of agent %s. %s",
                tripIndex, person.getId().toString(), appendix);
    }

    private String buildIllegalUtilityMessage(int tripIndex, Person person) {
        return String.format(
                "Received illegal utility for for tour starting at trip %d of agent %s. Continuing with next candidate.",
                tripIndex, person.getId().toString());
    }
}
