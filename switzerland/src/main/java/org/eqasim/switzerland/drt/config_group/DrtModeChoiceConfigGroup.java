package org.eqasim.switzerland.drt.config_group;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class DrtModeChoiceConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "DrtModeChoiceModule";

    public static final String IS_USE_WAITTIME = "isUseWaitTime";

    public static final String IS_USE_DELAYFACTOR = "isUseDelayFactor";
    public static final String FEEDBACK_METHOD = "feedbackMethod";

    public static final String WRITE_DETAILED_STATS = "writeDetailedStats";

    @Nullable
    DrtMetricSmootheningParamSet drtMetricSmootheningSettings;

    @Nullable
    DrtMetricCalculationParamSet drtMetricCalculationParamSet;

    public DrtModeChoiceConfigGroup() {
        super(GROUP_NAME);
        addDefinition(DrtMetricSmootheningParamSet.SET_NAME, DrtMetricSmootheningParamSet::new, () -> drtMetricSmootheningSettings,
                params -> drtMetricSmootheningSettings = (DrtMetricSmootheningParamSet) params);
        addDefinition(DrtMetricCalculationParamSet.SET_NAME, DrtMetricCalculationParamSet::new, () -> drtMetricCalculationParamSet,
                params -> drtMetricCalculationParamSet = (DrtMetricCalculationParamSet) params);

    }

    //add default values
    private boolean isUseWaitTime = true;
    private boolean isUseDelayFactor = true;

    private boolean writeDetailedStats = true;

    private String feedbackMethod = "average";
    //getters and setters
    @StringGetter(IS_USE_WAITTIME)
    public boolean isUseWaitTime() {
        return this.isUseWaitTime;
    }

    @StringSetter(IS_USE_WAITTIME)
    public void isUseWaitTime(boolean isUseWaitTime) {
        this.isUseWaitTime = isUseWaitTime;
    }

    @StringGetter(IS_USE_DELAYFACTOR)
    public boolean isUseDelayFactor() {
        return this.isUseDelayFactor;
    }

    @StringSetter(IS_USE_DELAYFACTOR)
    public void isUseDelayFactor(boolean isUseDelayFactor) {
        this.isUseDelayFactor = isUseDelayFactor;
    }

    @StringGetter(FEEDBACK_METHOD)
    public String getFeedBackMethod() {
        return this.feedbackMethod;
    }

    @StringSetter(FEEDBACK_METHOD)
    public void setFeedBackMethod(String feedbackMethod) {
        this.feedbackMethod = feedbackMethod;
    }

    @StringGetter(WRITE_DETAILED_STATS)
    public boolean writeDetailedStats() {
        return this.writeDetailedStats;
    }

    @StringSetter(WRITE_DETAILED_STATS)
    public void setWriteDetailedStats(boolean writeDetailedStats) {
        this.writeDetailedStats = writeDetailedStats;
    }

    @Nullable
    public DrtMetricSmootheningParamSet getDrtMetricSmootheningParamSet() {
        return drtMetricSmootheningSettings;
    }

    @Nullable
    public DrtMetricCalculationParamSet getDrtMetricCalculationParamSet() {
        return drtMetricCalculationParamSet;
    }





}

