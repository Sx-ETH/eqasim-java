package org.eqasim.switzerland.astra.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.switzerland.astra.variables.AstraWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;


public class AstraWalkPredictor extends CachedVariablePredictor<AstraWalkVariables> {
    public final WalkPredictor delegate;

    @Inject
    public AstraWalkPredictor(WalkPredictor delegate) {
        this.delegate = delegate;
    }

    @Override
    protected AstraWalkVariables predict(Person person, DiscreteModeChoiceTrip trip,
                                         List<? extends PlanElement> elements) {
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return new AstraWalkVariables(delegate.predictVariables(person, trip, elements), euclideanDistance_km);
    }
}
