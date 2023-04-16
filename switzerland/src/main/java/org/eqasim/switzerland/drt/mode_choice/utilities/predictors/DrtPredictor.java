package org.eqasim.switzerland.drt.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.drt.mode_choice.utilities.variables.DrtVariables;
import org.eqasim.switzerland.drt.travel_times.DrtPredictions;
import org.eqasim.switzerland.drt.travel_times.TravelTimeUpdates;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

public class DrtPredictor extends CachedVariablePredictor<DrtVariables> {
    private static final Logger log = LogManager.getLogger(DrtPredictor.class);
    private CostModel costModel;

    private final TravelTimeUpdates travelTimeUpdates;
    private DrtPredictions drtPredictions;

    @Inject
    public DrtPredictor(@Named("drt") CostModel costModel, TravelTimeUpdates travelTimeUpdates, DrtPredictions drtPredictions) {

        this.costModel = costModel;
        this.travelTimeUpdates = travelTimeUpdates;
        this.drtPredictions = drtPredictions;
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

                    travelTime_min = travelTimeUpdates.getTravelTime_Sec(route) / 60.0;
                    waitingTime_min = travelTimeUpdates.getWaitTime_Sec(route) / 60.0;

                    cost_MU = costModel.calculateCost_MU(person, trip, elements);

                    break;
                default:
                    throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
            }
        }

        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        this.drtPredictions.addTripPrediction(travelTime_min, accessEgressTime_min, cost_MU, waitingTime_min, euclideanDistance_km, person, trip);

        // todo add rejection penalty based on some probability of rejections

        return new DrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
    }
}