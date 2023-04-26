package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtMetricSmootheningParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "drtMetricSmootheningSettings";

    public static final String MSA_WEIGHT = "msaWeight";

    public static final String MOVING_WINDOW = "movingWindow";
    public static final String SMOOTHENING_TYPE = "smootheningType";

    //options are IterationBased, MovingAverage, SuccessiveAverage
    public enum SmootheningType {IterationBased, MovingAverage, SuccessiveAverage}

    private SmootheningType smootheningType = SmootheningType.IterationBased;

    @PositiveOrZero
    private double msaWeight = 0.0;

    @Positive
    private int movingWindow = 5;

    public DrtMetricSmootheningParamSet() {
        super(SET_NAME);
    }

    @StringGetter(MSA_WEIGHT)
    public double getMsaWeight() {
        return this.msaWeight;
    }

    //ToDo add conditions to the setters
    @StringSetter(MSA_WEIGHT)
    public void setMsaWeight(double msaWeight) {
        this.msaWeight = msaWeight;
    }

    @StringGetter(MOVING_WINDOW)
    public int getMovingWindow() {
        return this.movingWindow;
    }

    @StringSetter(MOVING_WINDOW)
    public void setMovingWindow(int movingWindow) {
        this.movingWindow = movingWindow;
    }

    @StringGetter(SMOOTHENING_TYPE)
    public SmootheningType getSmootheningType() {
        return this.smootheningType;
    }

    @StringSetter(SMOOTHENING_TYPE)
    public void setSmootheningType(SmootheningType smootheningType) {
        this.smootheningType = smootheningType;
    }
}
