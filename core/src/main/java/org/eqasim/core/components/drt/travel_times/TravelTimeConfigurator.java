package org.eqasim.core.components.drt.travel_times;

import org.eqasim.core.components.drt.config_group.*;
import org.matsim.core.config.Config;

public class TravelTimeConfigurator {
    public void TravelTimeConfigurator() {
    }

    public static void configureDrtTimeMetrics(Config config) {
        DrtModeChoiceConfigGroup drtDmcConfig = (DrtModeChoiceConfigGroup) config.getModules().get(DrtModeChoiceConfigGroup.GROUP_NAME);
        // drtDmcConfig.setFeedBackMethod("average");

        //...
        if (drtDmcConfig.getDrtMetricCalculationParamSet() == null) {
            DrtMetricCalculationParamSet calculationParamSet = new DrtMetricCalculationParamSet();
            calculationParamSet.addParameterSet(new DrtZonalSystemParamSet());
            calculationParamSet.addParameterSet(new DrtDynamicSystemParamSet());
            drtDmcConfig.addParameterSet(calculationParamSet);
        } else {
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getDrtZonalSystemParamSet() == null) {
                DrtZonalSystemParamSet zonalSystemParamSet = new DrtZonalSystemParamSet();
                drtDmcConfig.getDrtMetricCalculationParamSet().addParameterSet(zonalSystemParamSet);
            }
            if (drtDmcConfig.getDrtMetricCalculationParamSet().getDrtDynamicSystemParamSet() == null) {
                DrtDynamicSystemParamSet dynamicSystemParamSet = new DrtDynamicSystemParamSet();
                drtDmcConfig.getDrtMetricCalculationParamSet().addParameterSet(dynamicSystemParamSet);
            }
        }

        if (drtDmcConfig.getDrtMetricSmootheningParamSet() == null) {
            DrtMetricSmootheningParamSet smootheningParamSet = new DrtMetricSmootheningParamSet();
            drtDmcConfig.addParameterSet(smootheningParamSet);
        }

        //can then set up other metrics here if needed or set them up within the if statements

        //drtDmcConfig.getDrtMetricCalculationParamSet().setDistanceBin_m(500);
        //..and so on
    }
}
