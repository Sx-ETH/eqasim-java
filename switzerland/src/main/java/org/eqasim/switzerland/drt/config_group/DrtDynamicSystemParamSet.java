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

    private String type = "KNN_CP";
    public DrtDynamicSystemParamSet() {
        super(SET_NAME);
    }

    @StringGetter(TYPE)
    public String getType() {
        return this.type;
    }

    @StringSetter(TYPE)
    public void setType(String type) {
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
