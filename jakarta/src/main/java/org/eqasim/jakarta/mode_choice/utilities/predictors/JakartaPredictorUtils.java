package org.eqasim.jakarta.mode_choice.utilities.predictors;

import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaPredictorUtils {
//	static public boolean hasSubscription(Person person) {
//		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
//		return hasSubscription != null && hasSubscription;
//	}
	
//	static public boolean startsEndsinCity(DiscreteModeChoiceTrip trip) {
//		Boolean startInCity = (Boolean) trip.getOriginActivity().getAttributes().getAttribute("city");
//		Boolean endInCity = (Boolean) trip.getDestinationActivity().getAttributes().getAttribute("city");
//		return startInCity != null && endInCity != null && startInCity | endInCity;
//	}

	static public Double hhlIncome(Person person, JakartaModeParameters parameters) {
        Double hhlIncome = (Double) person.getAttributes().getAttribute("hhlIncome");
        if (hhlIncome != null) {
        	return hhlIncome;
        }
		return parameters.jAvgHHLIncome.avg_hhl_income;
	}
}
