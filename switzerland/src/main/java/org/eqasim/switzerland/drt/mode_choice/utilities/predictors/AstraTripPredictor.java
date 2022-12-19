package org.eqasim.switzerland.drt.mode_choice.utilities.predictors;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.drt.mode_choice.utilities.variables.AstraTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;


public class AstraTripPredictor extends CachedVariablePredictor<AstraTripVariables> {
	@Override
	protected AstraTripVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean isWork = AstraPredictorUtils.hasPurposeWork(trip.getOriginActivity())
				|| AstraPredictorUtils.hasPurposeWork(trip.getDestinationActivity());

		boolean isCity = AstraPredictorUtils.isInsideCity(trip.getOriginActivity())
				|| AstraPredictorUtils.isInsideCity(trip.getDestinationActivity());

		return new AstraTripVariables(isWork, isCity);
	}
}
