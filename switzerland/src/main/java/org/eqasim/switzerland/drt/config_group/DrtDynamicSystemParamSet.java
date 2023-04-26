package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtDynamicSystemParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "dynamicSystem";

    //different knn options
    public static final String TYPE = "type";

    public static final String K_VALUE = "kvalue";

    @PositiveOrZero
    private int kvalue = 0;

    public enum Type {KNN_CN, KNN_PN, FD}

    private Type type = Type.KNN_CN;

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
}
