package org.eqasim.switzerland.drt;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.annotation.Nullable;

public class DrtModeChoiceConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "DrtModeChoiceModule";

    public static final String IS_USE_WAITTIME = "isUseWaitTime";

    public static final String IS_USE_DELAYFACTOR = "isUseDelayFactor";
    public static final String IS_USE_WAIT_DELAY_METRICS = "isUseWaitandDelayMetrics";

    @Nullable
    DrtMetricSmootheningParamSet drtMetricSmootheningSettings;

    public DrtModeChoiceConfigGroup() {
        super(GROUP_NAME);
        addDefinition(DrtMetricSmootheningParamSet.SET_NAME, DrtMetricSmootheningParamSet::new, () -> drtMetricSmootheningSettings,
                params -> drtMetricSmootheningSettings = (DrtMetricSmootheningParamSet) params);
    }

    //add default values
    private boolean isUseWaitTime = false;
    private boolean isUseDelayFactor = false;
    private boolean isUseWaitandDelayMetrics = false;

    //getters and setters
    @StringGetter(IS_USE_WAITTIME)
    public boolean isUseWaitTime() {
        return this.isUseWaitTime;
    }

    @StringSetter(IS_USE_DELAYFACTOR)
    public void isUseWaitTime(boolean isUseDelayFactor) {
        this.isUseDelayFactor = isUseDelayFactor;
    }

    @StringGetter(IS_USE_DELAYFACTOR)
    public boolean isUseDelayFactor() {
        return this.isUseDelayFactor;
    }

    @StringSetter(IS_USE_DELAYFACTOR)
    public void isUseDelayFactor(boolean isUseDelayFactor) {
        this.isUseDelayFactor = isUseDelayFactor;
    }

    @StringGetter(IS_USE_WAIT_DELAY_METRICS)
    public boolean isUseWaitandDelayMetrics() {
        return isUseWaitandDelayMetrics;
    }

    @StringSetter(IS_USE_WAIT_DELAY_METRICS)
    public void isUseWaitandDelayMetrics(boolean isUseWaitandDelayMetrics) {
        isUseWaitandDelayMetrics = isUseWaitandDelayMetrics;
    }

    @Nullable
    public DrtMetricSmootheningParamSet getDrtMetricSmootheningParamSet() {
        return drtMetricSmootheningSettings;
    }





}

