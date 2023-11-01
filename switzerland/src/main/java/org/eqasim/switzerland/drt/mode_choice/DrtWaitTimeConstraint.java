package org.eqasim.switzerland.drt.mode_choice;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.drt.travel_times.TravelTimeUpdates;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DrtWaitTimeConstraint extends AbstractTripConstraint {
    static public final String NAME = "DrtWaitTimeConstraint";

    private final TravelTimeUpdates travelTimeUpdates;
    private final Scenario scenario;
    private Collection<String> drtModes;

    private static final Logger log = LogManager.getLogger(DrtWaitTimeConstraint.class);

    public static int waitTimeLogCount = 0;

    public DrtWaitTimeConstraint(TravelTimeUpdates travelTimeUpdates, Scenario scenario){

        this.travelTimeUpdates = travelTimeUpdates;
        this.scenario = scenario;

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) scenario.getConfig().getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
        drtModes = multiModeDrtConfigGroup.modes().collect(Collectors.toList());
    }

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
                                           List<TripCandidate> previousCandidates){

            for (PlanElement element : ((RoutedTripCandidate) candidate).getRoutedPlanElements()) {
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;
                    if (drtModes.contains(leg.getMode())) {
                        // If we find at least one drt leg, we're good
                        DrtRoute route = (DrtRoute) leg.getRoute();
                        double waitingTime_min = travelTimeUpdates.getWaitTime_sec(route, leg.getDepartureTime().seconds()) / 60.0;

                        if (waitingTime_min > 15.0) { //ToDo make this time adjustable from config file
                            waitTimeLogCount++;
                            if (waitTimeLogCount < 20) {
                                log.warn("rejecting drt trip as waiting time is too high");
                            }
                            return false;
                        }
                    }
                }
            }



        return true;

    }

    static public class Factory implements TripConstraintFactory {

        private final TravelTimeUpdates travelTimeUpdates;
        private final Scenario scenario;

        @Inject
        public Factory(TravelTimeUpdates travelTimeUpdates, Scenario scenario) {
            this.travelTimeUpdates = travelTimeUpdates;
            this.scenario = scenario;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
                                               Collection<String> availableModes) {
            return new DrtWaitTimeConstraint(travelTimeUpdates, scenario);
        }

    }
}
