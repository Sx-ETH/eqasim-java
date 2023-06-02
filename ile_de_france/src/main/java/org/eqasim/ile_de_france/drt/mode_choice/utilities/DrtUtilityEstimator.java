package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import com.google.inject.Inject;
import org.eqasim.core.components.drt.mode_choice.utilities.DrtPredictorInterface;
import org.eqasim.core.components.drt.mode_choice.utilities.DrtVariables;
import org.eqasim.core.components.drt.mode_choice.utilities.DrtVariablesComputedEvent;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtModeParameters;
import org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionPenaltyProvider;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.List;

public class DrtUtilityEstimator implements UtilityEstimator {
	private final IDFDrtModeParameters parameters;
	private final DrtPredictorInterface predictor;
	private final EventsManager eventsManager;
	private final DrtRejectionPenaltyProvider rejectionsPenaltyProvider;

	@Inject
	public DrtUtilityEstimator(IDFDrtModeParameters parameters, DrtPredictorInterface predictor, EventsManager eventsManager, DrtRejectionPenaltyProvider rejectionsPenaltyProvider) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.eventsManager = eventsManager;
		this.rejectionsPenaltyProvider = rejectionsPenaltyProvider;
	}

	protected double estimateConstantUtility() {
		return parameters.drt.alpha_u;
	}

	protected double estimateTravelTimeUtility(DrtVariables variables) {
		return parameters.drt.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateWaitingTimeUtility(DrtVariables variables) {
		return parameters.drt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(DrtVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateAccessEgressTimeUtility(DrtVariables variables) {
		return parameters.drt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		DrtVariables variables = predictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);


		//TODO this is fired way too often
		//TODO Because every chain of activity is evaluated
		//TODO Should fire it only for the one that is kept in the replanning
		this.eventsManager.processEvent(new DrtVariablesComputedEvent(0, person, trip, elements, variables));
		//TODO other idea, fire the event in the utility selector (be careful because we often switch between multinomial logit and maximum selector
		//TODO an option could be to create a selector that just fires events and delegates to implementations
		return utility + rejectionsPenaltyProvider.getRejectionPenalty();
	}
}
