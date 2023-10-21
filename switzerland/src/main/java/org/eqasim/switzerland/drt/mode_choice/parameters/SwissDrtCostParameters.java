package org.eqasim.switzerland.drt.mode_choice.parameters;


import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;

public class SwissDrtCostParameters extends SwissCostParameters {
    public double drtBaseFare_CHF;
    public double drtCost_CHF_km;

    public final static double COST_PER_DISTANCE_PER_VEHICLE_KM_DRT = 0.098;
    public final static double COST_PER_TRIP_DRT = 0.375;
    public final static double COST_PER_VEHICLE_PER_DAY_DRT = 33.3;

    public static SwissDrtCostParameters buildDefault() {

        SwissDrtCostParameters parameters = new SwissDrtCostParameters();

        parameters.carCost_CHF_km = 0.26;
        parameters.ptCost_CHF_km = 0.6;
        parameters.ptMinimumCost_CHF = 2.7;
        parameters.ptRegionalRadius_km = 15.0;

        parameters.drtBaseFare_CHF = 0; // 3;
        parameters.drtCost_CHF_km = 0.7; //0.56; //0.6 from sebastian for 4k fleet for 0 baseline fare in zurich city region

        return parameters;
    }

    public void setDrtPricePerKm(Double updatedDrtPrice) {
        this.drtCost_CHF_km = updatedDrtPrice;
    }

    public double getDrtPricePerKm() {
        return this.drtCost_CHF_km;
    }
}
