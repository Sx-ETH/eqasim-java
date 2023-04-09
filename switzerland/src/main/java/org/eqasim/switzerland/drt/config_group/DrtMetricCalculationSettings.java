package org.eqasim.switzerland.drt.config_group;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class DrtMetricCalculationSettings extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String SET_NAME = "drtMetricCalculation";

    public static final String METHOD = "method";

    public static final String SPATIAL_TYPE = "spatialType";
    public static final String TIME_BIN_MINUTE = "timeBinMin";

    public static final String DISTANCE_BIN_M = "distanceBin";

    @Nullable
    DrtDynamicSystemParamSet drtDynamicSystem;
    DrtZonalSystemParamSet drtZonalSystem;
    public DrtMetricCalculationSettings() {
        super(SET_NAME);
        addDefinition(DrtDynamicSystemParamSet.SET_NAME, DrtDynamicSystemParamSet::new, () -> drtDynamicSystem,
                params -> drtDynamicSystem = (DrtDynamicSystemParamSet) params);

        addDefinition(DrtZonalSystemParamSet.SET_NAME, DrtZonalSystemParamSet::new, () -> drtDynamicSystem,
                params -> drtZonalSystem = (DrtZonalSystemParamSet) params);

    }

    //add default values
    private String method = "SpatioTemporal";

    private String spatialType = "zonalSystem";
    private double timeBinMin = 30;
    private double distanceBin_m = 1500;

    //getters and setters
    @StringGetter(METHOD)
    public String method() {
        return this.method;
    }

    @StringGetter(SPATIAL_TYPE)
    public String getSpatialType() {
        return this.spatialType;
    }

    @StringGetter(TIME_BIN_MINUTE)
    public double getTimeBinMin() {
        return this.timeBinMin;
    }

    @StringGetter(DISTANCE_BIN_M)
    public double getDistanceBinMetres() {
        return this.distanceBin_m;
    }

    @Nullable
    public DrtDynamicSystemParamSet getDrtDynamicSystemParamSet() {
        return drtDynamicSystem;
    }

    @Nullable
    public DrtZonalSystemParamSet getDrtZonalystemParamSet() {
        return drtZonalSystem;
    }




}

