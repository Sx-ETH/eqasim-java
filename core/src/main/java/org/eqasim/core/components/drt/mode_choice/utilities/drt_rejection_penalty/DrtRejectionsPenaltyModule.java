package org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

public class DrtRejectionsPenaltyModule extends AbstractEqasimExtension {

    private final DrtRejectionPenaltyProviderConfigGroup configGroup;

    public DrtRejectionsPenaltyModule(DrtRejectionPenaltyProviderConfigGroup configGroup) {
        this.configGroup = configGroup;
    }

    @Override
    protected void installEqasimExtension() {
        bind(DrtRejectionsLinearPenaltyProviderConfigGroup.class).toInstance((DrtRejectionsLinearPenaltyProviderConfigGroup) configGroup.getPenaltyProviderParams());
        bind(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
        bind(DrtRejectionPenaltyProvider.class).to(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
        addControlerListenerBinding().to(DrtRejectionsLinearPenaltyProvider.class).asEagerSingleton();
        bind(RejectionTracker.class).asEagerSingleton();
        addEventHandlerBinding().to(RejectionTracker.class).asEagerSingleton();
    }
}
