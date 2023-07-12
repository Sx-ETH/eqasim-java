package org.eqasim.switzerland.astra.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.components.drt.travel_times.DrtPredictions;
import org.eqasim.core.components.drt.travel_times.TravelTimeUpdates;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.astra.variables.AstraDrtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

public class AstraDrtPredictor extends CachedVariablePredictor<AstraDrtVariables> {
    private CostModel costModel;
    private final TravelTimeUpdates travelTimeUpdates;
    private DrtPredictions drtPredictions;

    @Inject
    public AstraDrtPredictor(@Named("drt") CostModel costModel, TravelTimeUpdates travelTimeUpdates, DrtPredictions drtPredictions) {

        this.costModel = costModel;
        this.travelTimeUpdates = travelTimeUpdates;
        this.drtPredictions = drtPredictions;
    }

    @Override
    public AstraDrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = 0.0;
        double accessEgressTime_min = 0.0;
        double cost_MU = 0.0;
        double waitingTime_min = 0.0;
        double maxTravelTime_min = 0.0;
        double directRideTime_min = 0.0;
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            switch (leg.getMode()) {
                case TransportMode.walk:
                    accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
                    break;
                case "drt":
                    DrtRoute route = (DrtRoute) leg.getRoute();

                    travelTime_min = travelTimeUpdates.getTravelTime_sec(route, leg.getDepartureTime().seconds()) / 60.0;
                    waitingTime_min = travelTimeUpdates.getWaitTime_sec(route, leg.getDepartureTime().seconds()) / 60.0;

                    cost_MU = costModel.calculateCost_MU(person, trip, elements);

                    maxTravelTime_min = route.getMaxTravelTime() / 60.0;
                    directRideTime_min = route.getDirectRideTime() / 60.0;


                    this.drtPredictions.addTripPrediction(travelTime_min, accessEgressTime_min, cost_MU, waitingTime_min,
                            euclideanDistance_km, maxTravelTime_min, directRideTime_min, leg.getRoute().getStartLinkId(),
                            leg.getDepartureTime().seconds(), person, trip);
                    break;
                default:
                    throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
            }
        }
        // todo add rejection penalty based on some probability of rejections

        return new AstraDrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
    }
}
