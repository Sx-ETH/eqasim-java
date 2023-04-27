package org.eqasim.switzerland.drt.mode_choice;

import org.matsim.api.core.v01.network.Network;
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

    static private final double drtDistanceLimit_m = 250;

    private final Network network;

    public DrtDistanceConstraint(Network network) {
        this.network = network;
    }

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        if (mode == "drt") {

            var start = trip.getOriginActivity().getCoord();
            var stop = trip.getDestinationActivity().getCoord();

            //get the closest link to the trip origin and destination, link distances may be shorter as well
            var linkCoordStart = network.getLinks().get(trip.getOriginActivity().getLinkId()).getCoord();
            var linkCoordEnd = network.getLinks().get(trip.getDestinationActivity().getLinkId()).getCoord();


            if (CoordUtils.calcEuclideanDistance(linkCoordStart, linkCoordEnd) < drtDistanceLimit_m | (CoordUtils.calcEuclideanDistance(start, stop) < drtDistanceLimit_m)) {
                return false;
            }
        }

        return true;
    }

    static public class Factory implements TripConstraintFactory {
        private final Network network;

        public Factory(Network network){
            this.network = network;
        }
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
                                               Collection<String> availableModes) {
            return new DrtDistanceConstraint(network);
        }
    }
}
