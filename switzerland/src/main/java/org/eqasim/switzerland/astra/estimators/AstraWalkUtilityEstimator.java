package org.eqasim.switzerland.astra.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.switzerland.astra.AstraModeParameters;
import org.eqasim.switzerland.astra.predictors.AstraPersonPredictor;
import org.eqasim.switzerland.astra.predictors.AstraTripPredictor;
import org.eqasim.switzerland.astra.predictors.AstraWalkPredictor;
import org.eqasim.switzerland.astra.variables.AstraPersonVariables;
import org.eqasim.switzerland.astra.variables.AstraTripVariables;
import org.eqasim.switzerland.astra.variables.AstraWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;


public class AstraWalkUtilityEstimator extends WalkUtilityEstimator {
    static public final String NAME = "AstraWalkEstimator";

    private final AstraModeParameters parameters;
    private final AstraWalkPredictor predictor;
    private final AstraPersonPredictor personPredictor;
    private final AstraTripPredictor tripPredictor;

    @Inject
    public AstraWalkUtilityEstimator(AstraModeParameters parameters, AstraWalkPredictor predictor,
                                     AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
        super(parameters, predictor.delegate);

        this.parameters = parameters;
        this.predictor = predictor;
        this.personPredictor = personPredictor;
        this.tripPredictor = tripPredictor;
    }

    protected double estimateTravelTimeUtility(AstraWalkVariables variables) {
        return super.estimateTravelTimeUtility(variables) //
                * EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
                parameters.lambdaTravelTimeEuclideanDistance);
    }

    protected double estimateAgeUtility(AstraPersonVariables variables) {
        return variables.age_a >= 60 ? parameters.astraWalk.betaAgeOver60 : 0.0;
    }

    protected double estimateWorkUtility(AstraTripVariables variables) {
        return variables.isWork ? parameters.astraWalk.betaWork : 0.0;
    }

    protected double estimatePenalty(AstraWalkVariables variables) {
        double beta = Math.log(100) / parameters.astraWalk.travelTimeThreshold_min;
        return -Math.exp(beta * variables.travelTime_min) + 1.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        AstraWalkVariables variables = predictor.predictVariables(person, trip, elements);
        AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateAgeUtility(personVariables);
        utility += estimateWorkUtility(tripVariables);
        utility += estimatePenalty(variables);

        return utility;
    }
}
