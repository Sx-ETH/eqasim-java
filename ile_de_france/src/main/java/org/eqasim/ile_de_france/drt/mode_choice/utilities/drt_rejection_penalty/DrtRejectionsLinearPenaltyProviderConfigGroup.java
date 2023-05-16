package org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DrtRejectionsLinearPenaltyProviderConfigGroup extends ReflectiveConfigGroup implements DrtRejectionPenaltyProviderConfigGroup.DrtRejectionsPenaltyProviderParams{
    public static final String SET_NAME = "drtRejectionsLinearPenaltyProvider";

    public static final String ALPHA = "alpha";
    public static final String TARGET_REJECTION_PROBABILITY = "targetRejectionProbability";
    public static final String INITIAL_REJECTION_PENALTY = "initialRejectionPenalty";

    public static final String ENABLE_BACKWARD_ADJUSTMENT = "enableBackwardAdjustment";

    public DrtRejectionsLinearPenaltyProviderConfigGroup() {
        super(SET_NAME);
    }

    @PositiveOrZero
    private double targetRejectionProbability=0;

    @Positive
    private double alpha = 0.1;


    private double initialRejectionPenalty = 0.0;

    private boolean enableBackwardAdjustment = false;

    @StringGetter(TARGET_REJECTION_PROBABILITY)
    public double getTargetRejectionProbability() {
        return this.targetRejectionProbability;
    }

    @StringSetter(TARGET_REJECTION_PROBABILITY)
    public void setTargetRejectionProbability(double targetRejectionProbability) {
        this.targetRejectionProbability = targetRejectionProbability;
    }

    @StringGetter(ALPHA)
    public double getAlpha() {
        return this.alpha;
    }

    @StringSetter(ALPHA)
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @StringGetter(INITIAL_REJECTION_PENALTY)
    public double getInitialRejectionPenalty() {
        return this.initialRejectionPenalty;
    }

    @StringSetter(INITIAL_REJECTION_PENALTY)
    public void setInitialRejectionPenalty(double initialRejectionPenalty) {
        this.initialRejectionPenalty = initialRejectionPenalty;
    }

    @StringGetter(ENABLE_BACKWARD_ADJUSTMENT)
    public boolean isBackwardAdjustmentEnabled() {
        return this.enableBackwardAdjustment;
    }

    @StringSetter(ENABLE_BACKWARD_ADJUSTMENT)
    public void setEnableBackwardAdjustment(boolean enableBackwardAdjustment) {
        this.enableBackwardAdjustment = enableBackwardAdjustment;
    }

}
