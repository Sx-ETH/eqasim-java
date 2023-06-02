package org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty;

public class NoRejectionsPenalty implements DrtRejectionPenaltyProvider{
    @Override
    public double getRejectionPenalty() {
        return 0;
    }
}
