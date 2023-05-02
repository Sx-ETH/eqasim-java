package org.eqasim.switzerland.drt.config_group;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class DrtMetricCalculationParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String SET_NAME = "drtMetricCalculationSettings";

    public static final String METHOD = "method";

    public static final String SPATIAL_TYPE = "spatialType";
    public static final String TIME_BIN_MINUTE = "timeBinMin";

    public static final String DISTANCE_BIN_M = "distanceBin";
    public static final String LAST_BIN_START_DISTANCE_M = "lastBinStartDistance";

    @Nullable
    DrtDynamicSystemParamSet drtDynamicSystem;
    @Nullable
    DrtZonalSystemParamSet drtZonalSystem;

    public DrtMetricCalculationParamSet() {
        super(SET_NAME);
        addDefinition(DrtDynamicSystemParamSet.SET_NAME, DrtDynamicSystemParamSet::new, () -> drtDynamicSystem,
                params -> drtDynamicSystem = (DrtDynamicSystemParamSet) params);

        addDefinition(DrtZonalSystemParamSet.SET_NAME, DrtZonalSystemParamSet::new, () -> drtZonalSystem,
                params -> drtZonalSystem = (DrtZonalSystemParamSet) params);

    }

    //add default values
    public enum Method {SpatioTemporal, Spatio, Temporal, Global}

    private Method method = Method.SpatioTemporal;

    public enum SpatialType {ZonalSystem, DynamicSystem}

    private SpatialType spatialType = SpatialType.ZonalSystem;
    private int timeBinMin = 30;
    private int distanceBin_m = 1500;
    private int lastBinStartDistance_m = 10000;

    //getters and setters
    @StringGetter(METHOD)
    public Method getMethod() {
        return this.method;
    }

    @StringSetter(METHOD)
    public void setMethod(Method method) {
        this.method = method;
    }

    @StringGetter(SPATIAL_TYPE)
    public SpatialType getSpatialType() {
        return this.spatialType;
    }

    @StringSetter(SPATIAL_TYPE)
    public void setSpatialType(SpatialType spatialType) {
        this.spatialType = spatialType;
    }

    @StringGetter(TIME_BIN_MINUTE)
    public int getTimeBinMin() {
        return this.timeBinMin;
    }

    @StringSetter(TIME_BIN_MINUTE)
    public void setTimeBinMin(int timeBinMin) {
        this.timeBinMin = timeBinMin;
    }

    @StringGetter(DISTANCE_BIN_M)
    public int getDistanceBinMetres() {
        return this.distanceBin_m;
    }

    @StringSetter(DISTANCE_BIN_M)
    public void setDistanceBin_m(int distanceBin_m) {
        this.distanceBin_m = distanceBin_m;
    }

    @StringGetter(LAST_BIN_START_DISTANCE_M)
    public int getLastBinStartDistance_m() {
        return this.lastBinStartDistance_m;
    }

    @StringSetter(LAST_BIN_START_DISTANCE_M)
    public void setLastBinStartDistance_m(int lastBinStartDistance_m) {
        this.lastBinStartDistance_m = lastBinStartDistance_m;
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

