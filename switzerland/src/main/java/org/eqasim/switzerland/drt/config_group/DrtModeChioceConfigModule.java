package org.eqasim.switzerland.drt.config_group;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.core.config.CommandLine;

public class DrtModeChioceConfigModule extends AbstractEqasimExtension {

    DrtModeChoiceConfigGroup drtDmcConfig;
    private final CommandLine cmd;
    public DrtModeChioceConfigModule(CommandLine cmd, DrtModeChoiceConfigGroup drtDmcConfig){
        this.drtDmcConfig = drtDmcConfig;
        this.cmd = cmd;

    }
    @Override
    protected void installEqasimExtension() {
        //get the param set this.drtChoiceConfig.getDrtMetricSmootheningParamSet();
        //check if it is null  and if not null check if it has a child (the parameterset under it
        //if it does initiate all that needs to make the algorithm work

        DrtMetricCalculationParamSet drtMetrics = drtDmcConfig.getDrtMetricCalculationParamSet();
        if(drtMetrics != null){
            bind(DrtMetricCalculationParamSet.class).asEagerSingleton();
            bind(DrtMetricSmootheningParamSet.class).asEagerSingleton();
            //do we want to bind the controller listener and time trackers here?
            //since they would try to load these settings and if null may not work...

            if(drtMetrics.getDrtDynamicSystemParamSet() != null){
                //do something
            }

            if(drtMetrics.getDrtZonalSystemParamSet() != null){
                //do something
            }
            //ToDo put a condition that if both zonal and dynamic are null

        }
    }
}
