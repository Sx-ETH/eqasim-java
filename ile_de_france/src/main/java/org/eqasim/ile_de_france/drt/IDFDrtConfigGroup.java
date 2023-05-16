package org.eqasim.ile_de_france.drt;

import org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionPenaltyProviderConfigGroup;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class IDFDrtConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "IDFDrtModule";

    public static final String USE_FEEDER = "useFeeder";

    public static final String OVERRIDE_PCU = "overridePcu";

    public IDFDrtConfigGroup() {
        super(GROUP_NAME);
        addDefinition(DrtRejectionPenaltyProviderConfigGroup.SET_NAME, DrtRejectionPenaltyProviderConfigGroup::new, () -> drtRejectionPenaltyProvider,
                params -> drtRejectionPenaltyProvider = (DrtRejectionPenaltyProviderConfigGroup) params);

    }

    private boolean useFeeder = false;

    private Double overridePcu = 1.0;

    @Nullable
    DrtRejectionPenaltyProviderConfigGroup drtRejectionPenaltyProvider;


    @StringGetter(USE_FEEDER)
    public boolean isUsingFeeder() {
        return this.useFeeder;
    }

    @StringSetter(USE_FEEDER)
    public void setUsingFeeder(boolean useFeeder) {
        this.useFeeder = useFeeder;
    }

    @StringGetter(OVERRIDE_PCU)
    public Double getOverridePcu() {
        return this.overridePcu;
    }

    @StringSetter(OVERRIDE_PCU)
    public void setOverridePcu(double overridePcu) {
        this.overridePcu = overridePcu;
    }

    @Nullable
    public DrtRejectionPenaltyProviderConfigGroup getDrtRejectionPenaltyProviderConfig() {
        return drtRejectionPenaltyProvider;
    }
}

