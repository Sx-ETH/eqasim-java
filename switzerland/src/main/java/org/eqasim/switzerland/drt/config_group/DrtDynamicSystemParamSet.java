package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtDynamicSystemParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "dynamicSystem";

    //different knn options
    public static final String TYPE = "type";

    public static final String K_VALUE = "kvalue";
    public static final String RADIUS = "radius";

    public static final String K_SHARE = "kShare";
    private static final String K_MAX = "kMax";
    private static final String DECAY_TYPE = "decayType";

    @PositiveOrZero
    private int kvalue = 0;

    public enum Type {KNN_CN, KNN_PN, FD}

    public enum DecayType {POWER_DECAY, INVERSE_DECAY, EXPONENTIAL_DECAY, SPATIAL_CORRELATION}

    private Type type = Type.KNN_CN;

    private int kMax = 1000; //max k value that is needed for percentage share

    private double kShare = 0.0; //Todo Restriction set
    private DecayType decayType = DecayType.POWER_DECAY;

    @PositiveOrZero
    private double radius = 0.0; //ToDo make -1 to force user to set it?

    public DrtDynamicSystemParamSet() {
        super(SET_NAME);
    }

    @StringGetter(TYPE)
    public Type getType() {
        return this.type;
    }

    @StringSetter(TYPE)
    public void setType(Type type) {
        this.type = type;
    }

    @StringGetter(K_VALUE)
    public int getKvalue() {
        return this.kvalue;
    }

    @StringSetter(K_VALUE)
    public void setKvalue(int kvalue) {
        this.kvalue = kvalue;
    }

    @StringGetter(RADIUS)
    public double getRadius() {
        return radius;
    }

    @StringSetter(RADIUS)
    public void setRadius(double radius) {
        this.radius = radius;
    }

    @StringGetter(K_SHARE)
    public double getkShare() {
        return kShare;
    }

    @StringSetter(K_SHARE)
    public void setkShare(double kShare) {
        this.kShare = kShare;
    }

    @StringGetter(K_MAX)
    public int getkMax() {
        return this.kMax;
    }

    @StringSetter(K_MAX)
    public void setkMax(int kMax) {
        this.kMax = kMax;
    }

    @StringGetter(DECAY_TYPE)
    public DecayType getDecayType() {
        return decayType;
    }

    @StringSetter(DECAY_TYPE)
    public void setDecayType(DecayType decayType) {
        this.decayType = decayType;
    }
}
