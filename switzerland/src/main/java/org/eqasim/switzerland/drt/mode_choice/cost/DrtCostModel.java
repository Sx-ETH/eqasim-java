package org.eqasim.switzerland.drt.mode_choice.cost;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtCostModel extends AbstractCostModel {
    static public final String NAME = "DrtCostModel";
    private final SwissDrtCostParameters parameters;

    public DrtCostModel(SwissDrtCostParameters parameters) {
        super("drt");
        this.parameters = parameters;
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double tripDistance_km = getInVehicleDistance_km(elements);
        return parameters.drtCost_CHF + parameters.drtCost_CHF_km * tripDistance_km; //Todo add other pricing schemes
    }
}
