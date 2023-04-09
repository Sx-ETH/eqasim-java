package org.eqasim.switzerland.drt.config_group;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

public class DrtModeChioceConfigModule extends AbstractEqasimExtension {

    DrtModeChoiceConfigGroup drtChoiceConfig;
    public DrtModeChioceConfigModule(DrtModeChoiceConfigGroup drtChoiceConfig){
        this.drtChoiceConfig = drtChoiceConfig;

    }
    @Override
    protected void installEqasimExtension() {
        //get the param set this.drtChoiceConfig.getDrtMetricSmootheningParamSet();
        //check if it is null  and if not null check if it has a child (the parameterset under it
        //if it does initiate all that needs to make the algorithm work
    }
}
