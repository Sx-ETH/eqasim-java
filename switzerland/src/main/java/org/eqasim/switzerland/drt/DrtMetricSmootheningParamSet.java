package org.eqasim.switzerland.drt;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtMetricSmootheningParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "drtMetricSmootheningSettings";

    //options are IterationBased, MovingAverage, SuccessiveAverage
    public static final String name = "IterationBased";

    @PositiveOrZero
    public static final double msaWeight = 0.0;

    @Positive
    public static final int movingWindow = 5;
    public DrtMetricSmootheningParamSet() {
        super(SET_NAME);
    }
}
