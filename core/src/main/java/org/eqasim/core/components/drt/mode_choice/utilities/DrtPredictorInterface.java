package org.eqasim.core.components.drt.mode_choice.utilities;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public interface DrtPredictorInterface {

    DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements);
}
