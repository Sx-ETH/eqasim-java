package org.eqasim.ile_de_france.feeder;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.Collection;
import java.util.List;

public class FeederConstraint implements TripConstraint {
	public final static String NAME = "FeederConstraint";

	private final Facility interactionFacility;
	private final TransitSchedule schedule;

	private FeederConstraint(Facility interactionFacility, TransitSchedule schedule) {
		this.interactionFacility = interactionFacility;
		this.schedule = schedule;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if(trip.getOriginActivity().getType().equals("outside") || trip.getDestinationActivity().getType().equals("outside")) {
			if(trip.getOriginActivity().getType().equals(trip.getDestinationActivity().getType())) {
				return trip.getInitialMode().equals(mode);
			}
			if(trip.getInitialMode().equals("pt") || trip.getInitialMode().equals("feeder")) {
				return mode.equals("pt") || mode.equals("feeder");
			} else {
				return trip.getInitialMode().equals(mode);
			}
		}
		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().equals(FeederModule.FEEDER_MODE)) {
			RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) candidate;
			List<? extends PlanElement> elements = routedTripCandidate.getRoutedPlanElements();

			boolean foundPt = false;
			boolean foundDrt = false;

			for (PlanElement element : elements) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals(TransportMode.pt)) {
						foundPt = true;
					}
					else if(leg.getMode().equals(TransportMode.drt)) {
						foundDrt = true;
					}
				}
			}
			return foundPt && foundDrt;
		}
		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final Facility interactionFacility;
		private final TransitSchedule schedule;

		public Factory(Facility interactionFacility, TransitSchedule schedule) {
			this.interactionFacility = interactionFacility;
			this.schedule = schedule;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new FeederConstraint(interactionFacility, schedule);
		}
	}
}
