package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtZonalSystemParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "zonalSystem";

    //options
    public static final String zonesGeneration = "GridFromNetwork";
    @PositiveOrZero
    public static final int cellSize = 500;

    public static final String cellShape = "Hexagon";

    public static final String shapefile = "";

    public DrtZonalSystemParamSet() {
        super(SET_NAME);
    }
}
