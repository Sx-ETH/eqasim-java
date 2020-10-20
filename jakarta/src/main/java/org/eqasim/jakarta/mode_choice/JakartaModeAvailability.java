package org.eqasim.jakarta.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class JakartaModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);
		//modes.add(TransportMode.motorcycle);
		//modes.add(TransportMode.bike);
		//modes.add(TransportMode.taxi);
		modes.add("carodt");
		modes.add("mcodt");
		
		
		// Check car availability
		boolean carAvailability = true;

		if ("never".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
			carAvailability = false;
		}
		
		if ((int)person.getAttributes().getAttribute("age") < 18)
			carAvailability = false;

		if (carAvailability) {
			modes.add(TransportMode.car);
		}
		
		
		
		
		// Check motorcycle availability
		boolean motorcycleAvailability = true;
		
		//if ("never".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
		//	motorcycleAvailability = false;
		//}

		
	    if ((int)person.getAttributes().getAttribute("age") < 18)
	    	motorcycleAvailability = false;

		if (motorcycleAvailability) {
			modes.add(TransportMode.motorcycle);
		}
		
		
		
		// Add special mode "outside" if applicable
		Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");

		if (isOutside != null && isOutside) {
			modes.add("outside");
		}

		// Add special mode "car_passenger" if applicable
		Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isPassenger");

		if (isCarPassenger != null && isCarPassenger) {
			modes.add("car_passenger");
		}

		return modes;
	}
}