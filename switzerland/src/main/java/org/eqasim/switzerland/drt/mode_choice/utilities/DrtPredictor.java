package org.eqasim.switzerland.drt.mode_choice.utilities;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.drt.travel_times.detour_time.DrtDetourTimes;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtWaitTimes;
import org.eqasim.switzerland.drt.travel_times.wait_time.WayneCountyDrtZonalSystem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

public class DrtPredictor extends CachedVariablePredictor<DrtVariables> {
    private CostModel costModel;
    private WayneCountyDrtZonalSystem zones;
    private final DrtWaitTimes drtWaitTimes;

    private final DrtDetourTimes drtDetourTimes;

    @Inject
    public DrtPredictor(@Named("drt") CostModel costModel, WayneCountyDrtZonalSystem zones, DrtWaitTimes drtWaitTimes, DrtDetourTimes drtDetourtimes, DrtDetourTimes drtDetourTimes) {

        this.costModel = costModel;
        this.zones = zones;
        this.drtWaitTimes = drtWaitTimes;
        this.drtDetourTimes = drtDetourTimes;
    }

    @Override
    public DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = 0.0;
        double accessEgressTime_min = 0.0;
        double cost_MU = 0.0;
        double waitingTime_min = 0.0;
        boolean useAverageWaitTime = true; //toDo cmd config should say to use avg wait time or not
        boolean useDelayFactor = true; //todo transfer to config

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            switch (leg.getMode()) {
                case TransportMode.walk:
                    accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
                    break;
                case "drt":
                    DrtRoute route = (DrtRoute) leg.getRoute();


                    // Travel time is the max travel time set based on alpha*tt + beta (not actual)
                    // same for wait time which just uses maximum setting


                    travelTime_min = route.getMaxTravelTime() / 60.0;
                    route.getDirectRideTime();
                    waitingTime_min = route.getMaxWaitTime() / 60.0;

                    if (useDelayFactor) {

                    }

                    // Todo drt wait time and travel time update

                    if (useAverageWaitTime){
                        Id<Link> startLinkId = leg.getRoute().getStartLinkId();
                        String zone = this.zones.getZoneForLinkId(startLinkId);
                        drtWaitTimes.getAvgWaitTimes();

                        if (drtWaitTimes.getAvgWaitTimes().get(zone) != null) {
                            int index = (int) Math.floor(leg.getDepartureTime().seconds() / 3600.0);
                            waitingTime_min = this.drtWaitTimes.getAvgWaitTimes().get(zone)[index] / 60;
                            //toDo address zero wait times - check how to treat zones with no observations i.e zero wait time
                        }
                    }

                    cost_MU = costModel.calculateCost_MU(person, trip, elements);

                    break;
                default:
                    throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
            }
        }

        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        // todo add rejection penalty based on some probability of rejections

        return new DrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
    }
}