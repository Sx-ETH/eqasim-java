package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPtPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissTripPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissPtUtilityEstimator extends PtUtilityEstimator {
    private final SwissModeParameters swissModeParameters;
    private final SwissPersonPredictor swissPersonPredictor;
    private final SwissTripPredictor swissTripPredictor;
    private final SwissPtPredictor swissPtPredictor;


    @Inject
    public SwissPtUtilityEstimator(SwissModeParameters swissModeParameters, SwissPersonPredictor swissPersonPredictor,
                                   SwissTripPredictor swissTripPredictor, SwissPtPredictor swissPtPredictor) {
        super(swissModeParameters, swissPtPredictor.delegate);
        this.swissModeParameters = swissModeParameters;
        this.swissPersonPredictor = swissPersonPredictor;
        this.swissTripPredictor = swissTripPredictor;
        this.swissPtPredictor = swissPtPredictor;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return swissModeParameters.swissPt.betaAge * personVariables.age;
    }

    protected double estimateFemaleUtility(SwissPersonVariables personVariables) {
        if (personVariables.isFemale){
            return swissModeParameters.swissPt.betaIsFemale;}
        else {
            return 0.0;
        }
    }

    protected double estimateWorkTripUtility(SwissTripVariables tripVariables) {
        if (tripVariables.isWorkTrip){
            return swissModeParameters.swissPt.betaIsWorkTrip;}
        else {
            return 0.0;
        }
    }

    protected double estimateAccessEgressTimeUtility(SwissPtVariables ptVariables) {
        return swissModeParameters.swissPt.betaAccessEgressTime_hour * ptVariables.accessEgressTime_min/60;
    }

    protected double estimateInVehicleTimeUtility(SwissPtVariables ptVariables) {
        return swissModeParameters.swissPt.betaInVehicleTime_hour * ptVariables.inVehicleTime_min/60;
    }

    protected double estimateMonetaryCostUtility(SwissPtVariables ptVariables) {
        return swissModeParameters.betaCost_RD * EstimatorUtils.interaction(ptVariables.routedDistance, //g/ what unit is distance?
                swissModeParameters.referenceRoutedDistance_km, swissModeParameters.lambdaCostRoutedDistance) * ptVariables.cost_MU;
    }


    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = swissPersonPredictor.predictVariables(person, trip, elements);
        SwissTripVariables tripVariables = swissTripPredictor.predictVariables(person, trip, elements);
        SwissPtVariables ptVariables = swissPtPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += super.estimateUtility(person, trip, elements);
        utility += estimateAgeUtility(personVariables);
        utility += estimateFemaleUtility(personVariables);
        utility += estimateWorkTripUtility(tripVariables);
        utility += estimateAccessEgressTimeUtility(ptVariables);
        utility += estimateInVehicleTimeUtility(ptVariables);
        utility += estimateMonetaryCostUtility(ptVariables);


        return utility;
    }
}