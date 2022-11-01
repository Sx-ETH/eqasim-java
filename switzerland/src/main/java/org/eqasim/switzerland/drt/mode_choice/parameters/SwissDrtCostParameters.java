package org.eqasim.switzerland.drt.mode_choice.parameters;


import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;

public class SwissDrtCostParameters extends SwissCostParameters {
    public double drtCost_CHF;

    public static SwissDrtCostParameters buildDefault() {

        SwissDrtCostParameters parameters = new SwissDrtCostParameters();

        parameters.drtCost_CHF = 0;

        return parameters;
    }
}
