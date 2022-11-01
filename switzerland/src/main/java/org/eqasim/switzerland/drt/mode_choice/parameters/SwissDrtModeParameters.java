package org.eqasim.switzerland.drt.mode_choice.parameters;


import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class SwissDrtModeParameters extends SwissModeParameters {
    public class ZurichDrtParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double betaWaitingTime_u_min = 0.0;
        public double betaAccessEgressTime_u_min = 0.0;
        //Todo add more if necessary when astra2022 pooling project is done
    }

    public ZurichDrtParameters drt = new ZurichDrtParameters();

    public static SwissDrtModeParameters buildASTRA2016() {
        SwissDrtModeParameters parameters = new SwissDrtModeParameters();

        // DRT (adapted from public transport)
        parameters.drt.alpha_u = 0.0;
        parameters.drt.betaWaitingTime_u_min = -0.0;
        parameters.drt.betaTravelTime_u_min = -0.0;
        parameters.drt.betaAccessEgressTime_u_min = -0.0;
        //...

        // Cost
        parameters.betaCost_u_MU = -0.126;
        parameters.lambdaCostEuclideanDistance = -0.4;
        parameters.referenceEuclideanDistance_km = 40.0;

        // Car
        parameters.car.alpha_u = 0.827;
        parameters.car.betaTravelTime_u_min = -0.0667;

        parameters.car.constantAccessEgressWalkTime_min = 6.0;
        parameters.car.constantParkingSearchPenalty_min = 5.0;

        // PT
        parameters.pt.alpha_u = 0.0;
        parameters.pt.betaLineSwitch_u = -0.17;
        parameters.pt.betaInVehicleTime_u_min = -0.0192;
        parameters.pt.betaWaitingTime_u_min = -0.0384;
        parameters.pt.betaAccessEgressTime_u_min = -0.0804;

        // Bike
        parameters.bike.alpha_u = -0.1;
        parameters.bike.betaTravelTime_u_min = -0.0805;
        parameters.bike.betaAgeOver18_u_a = -0.0496;

        // Walk
        parameters.walk.alpha_u = 0.63;
        parameters.walk.betaTravelTime_u_min = -0.141;

        return parameters;
    }
}
