package org.eqasim.switzerland.astra.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.switzerland.astra.AstraModeParameters;
import org.eqasim.switzerland.astra.predictors.AstraDrtPredictor;
import org.eqasim.switzerland.astra.predictors.AstraPersonPredictor;
import org.eqasim.switzerland.astra.predictors.AstraTripPredictor;
import org.eqasim.switzerland.astra.variables.AstraDrtVariables;
import org.eqasim.switzerland.astra.variables.AstraPersonVariables;
import org.eqasim.switzerland.astra.variables.AstraTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class AstraDrtUtilityEstimator implements UtilityEstimator {
    static public final String NAME = "AstraDrtEstimator";

    private final AstraModeParameters parameters;
    private final AstraDrtPredictor predictor;
    private final AstraPersonPredictor personPredictor;
    private final AstraTripPredictor tripPredictor;

    @Inject
    public AstraDrtUtilityEstimator(AstraModeParameters parameters, AstraDrtPredictor predictor, AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
        this.parameters = parameters;
        this.predictor = predictor;
        this.personPredictor = personPredictor;
        this.tripPredictor = tripPredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.drt.alpha_u;
    }

    protected double estimateTravelTimeUtility(AstraDrtVariables variables) {
        return parameters.drt.betaTravelTime_u_min * variables.travelTime_min;
    }

    protected double estimateWaitingTimeUtility(AstraDrtVariables variables) {
        return parameters.drt.betaWaitingTime_u_min * variables.waitingTime_min;
    }

    protected double estimateMonetaryCostUtility(AstraDrtVariables variables, AstraPersonVariables personVariables) {
        return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
                parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance)
                * EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
                parameters.lambdaCostHouseholdIncome) * variables.cost_MU;
    }

    protected double estimateAccessEgressTimeUtility(AstraDrtVariables variables) {
        return parameters.drt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
    }

    protected double estimateWorkAgeconstants(AstraPersonVariables personVariables, AstraTripVariables tripVariables) {
        //for age
        double ageUtility = personVariables.age_a >= 60 ? parameters.astraDrt.betaAgeOver60 : 0.0;

        double workUtility = tripVariables.isWork ? parameters.astraDrt.betaWork : 0.0;

        return ageUtility + workUtility;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        AstraDrtVariables drtVariables = predictor.predict(person, trip, elements);
        AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(drtVariables);
        utility += estimateWaitingTimeUtility(drtVariables);
        utility += estimateAccessEgressTimeUtility(drtVariables);
        utility += estimateMonetaryCostUtility(drtVariables, personVariables);
        utility += estimateWorkAgeconstants(personVariables, tripVariables);

        return utility;
    }
}
