package org.eqasim.switzerland.drt.mode_choice;

import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

import java.util.Collection;
import java.util.List;


public class SwissDrtModeAvailability implements ModeAvailability {
    static public final String NAME = "ZurichDrtModeAvailability";

    private final ModeAvailability delegate = new SwissModeAvailability();

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = delegate.getAvailableModes(person, trips);

        if (modes.contains(TransportMode.walk)) {
            modes.add("drt");
        }
        //Todo set constraints if any needed

        return modes;
    }
}
