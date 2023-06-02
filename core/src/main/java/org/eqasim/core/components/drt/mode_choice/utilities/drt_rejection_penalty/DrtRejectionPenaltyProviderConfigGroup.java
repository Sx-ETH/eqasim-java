package org.eqasim.core.components.drt.mode_choice.utilities.drt_rejection_penalty;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

public class DrtRejectionPenaltyProviderConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String SET_NAME = "drtRejectionPenaltyProvider";

    public interface DrtRejectionsPenaltyProviderParams {

    }

    private DrtRejectionsPenaltyProviderParams penaltyProviderParams = null;

    public DrtRejectionPenaltyProviderConfigGroup() {
        super(SET_NAME);
        addDefinition(DrtRejectionsLinearPenaltyProviderConfigGroup.SET_NAME, DrtRejectionsLinearPenaltyProviderConfigGroup::new,
                () -> (ConfigGroup) penaltyProviderParams,
                params -> {
                    penaltyProviderParams = (DrtRejectionsLinearPenaltyProviderConfigGroup) params;
                });
    }


    public DrtRejectionsPenaltyProviderParams getPenaltyProviderParams() {
        return this.penaltyProviderParams;
    }
}
