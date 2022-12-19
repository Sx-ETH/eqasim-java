package org.eqasim.switzerland.drt.mode_choice.utilities.predictors;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.drt.mode_choice.utilities.variables.DrtVariables;
import org.eqasim.switzerland.drt.travel_times.TravelTimeUpdates;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DrtPredictor extends CachedVariablePredictor<DrtVariables> {
	private static final Logger log = LogManager.getLogger(DrtPredictor.class);
	private CostModel costModel;

	private final TravelTimeUpdates travelTimeUpdates;

	@Inject
	public DrtPredictor(@Named("drt") CostModel costModel, TravelTimeUpdates travelTimeUpdates) {

		this.costModel = costModel;
		this.travelTimeUpdates = travelTimeUpdates;
	}

	@Override
	public DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double travelTime_min = 0.0;
		double accessEgressTime_min = 0.0;
		double cost_MU = 0.0;
		double waitingTime_min = 0.0;
		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			switch (leg.getMode()) {
			case TransportMode.walk:
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
				break;
			case "drt":
				DrtRoute route = (DrtRoute) leg.getRoute();

				travelTime_min = travelTimeUpdates.getTravelTime(route);
				waitingTime_min = travelTimeUpdates.getWaitTime(route);

				cost_MU = costModel.calculateCost_MU(person, trip, elements);

				break;
			default:
				throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		log.warn("Drt Variables: " + person.getId().toString() + ": " + String.valueOf(travelTime_min) +  " " + String.valueOf(cost_MU) + " " + String.valueOf(euclideanDistance_km) + " " + String.valueOf(waitingTime_min) + " " + String.valueOf(accessEgressTime_min));

		// todo add rejection penalty based on some probability of rejections

		return new DrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
	}
}