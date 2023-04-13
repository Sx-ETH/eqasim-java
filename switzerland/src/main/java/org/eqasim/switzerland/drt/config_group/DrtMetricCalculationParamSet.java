package org.eqasim.switzerland.drt.config_group;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class DrtMetricCalculationParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String SET_NAME = "drtMetricCalculationSettings";

    public static final String METHOD = "method";

    public static final String SPATIAL_TYPE = "spatialType";
    public static final String TIME_BIN_MINUTE = "timeBinMin";

    public static final String DISTANCE_BIN_M = "distanceBin";

    @Nullable
    DrtDynamicSystemParamSet drtDynamicSystem;
    @Nullable
    DrtZonalSystemParamSet drtZonalSystem;
    public DrtMetricCalculationParamSet() {
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
    public String getMethod() {
        return this.method;
    }
    @StringSetter(METHOD)
    public void setMethod(String method) {
        this.method = method;
    }
    @StringGetter(SPATIAL_TYPE)
    public String getSpatialType() {
        return this.spatialType;
    }

    @StringSetter(SPATIAL_TYPE)
    public void setSpatialType(String spatialType) {
        this.spatialType = spatialType;
    }

    @StringGetter(TIME_BIN_MINUTE)
    public double getTimeBinMin() {
        return this.timeBinMin;
    }

    @StringSetter(TIME_BIN_MINUTE)
    public void setTimeBinMin(double timeBinMin) {
        this.timeBinMin = timeBinMin;
    }

    @StringGetter(DISTANCE_BIN_M)
    public double getDistanceBinMetres() {
        return this.distanceBin_m;
    }

    @StringSetter(DISTANCE_BIN_M)
    public void setDistanceBin_m(double distanceBin_m) {
        this.distanceBin_m = distanceBin_m;
    }

    @Nullable
    public DrtDynamicSystemParamSet getDrtDynamicSystemParamSet() {
        return drtDynamicSystem;
    }

    @Nullable
    public DrtZonalSystemParamSet getDrtZonalSystemParamSet() {
        return drtZonalSystem;
    }




}

