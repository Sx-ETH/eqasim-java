package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtDynamicSystemParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "dynamicSystem";

    //different knn options
    public static final String type = "KNN_CP";

    @PositiveOrZero
    public static final int kvalue = 0;

    @Positive
    public static final int movingWindow = 5;
    public DrtDynamicSystemParamSet() {
        super(SET_NAME);
    }
}
