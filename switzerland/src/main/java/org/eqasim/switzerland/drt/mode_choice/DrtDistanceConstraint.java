package org.eqasim.switzerland.drt.mode_choice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.List;

public class DrtDistanceConstraint extends AbstractTripConstraint {
    static public final String NAME = "DrtDistanceConstraint";

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        if (mode == "drt") {

            var start = trip.getOriginActivity().getCoord();
            var stop = trip.getDestinationActivity().getCoord();

            return !(CoordUtils.calcEuclideanDistance(start, stop) < 250);
        }

        return true;
    }

    static public class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
                                               Collection<String> availableModes) {
            return new DrtDistanceConstraint();
        }
    }
}
