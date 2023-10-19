package org.eqasim.switzerland.drt.mode_choice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.List;

public class DrtWalkConstraint extends AbstractTripConstraint {
    static public final String NAME = "DrtWalkConstraint";

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
                                           List<TripCandidate> previousCandidates) {
        Collection<String> drtModes = List.of(TransportMode.drt); //ToDo make it not fixed mode
        if (drtModes.contains(candidate.getMode())) {
            if (candidate instanceof RoutedTripCandidate) {
                // Go through all plan elements
                for (PlanElement element : ((RoutedTripCandidate) candidate).getRoutedPlanElements()) {
                    if (element instanceof Leg) {
                        if (drtModes.contains(((Leg) element).getMode())) {
                            // If we find at least one drt leg, we're good
                            return true;
                        }
                    }
                }

                // If there was no pt leg, we do not accept this candidate
                return false;
            } else {
                throw new IllegalStateException("Need a route to evaluate constraint");
            }

        }
        return true;
    }

    static public class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
                                               Collection<String> availableModes) {
            return new DrtWalkConstraint();
        }
    }

}

