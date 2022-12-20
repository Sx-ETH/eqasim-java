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

    static public class AstraBaseModeParameters {
        public double betaAgeOver60 = 0.0;
        public double betaWork = 0.0;
        public double betaCity = 0.0;

        public double travelTimeThreshold_min = 0.0;
    }
    public AstraBaseModeParameters astraWalk = new AstraBaseModeParameters();
    public AstraBaseModeParameters astraBike = new AstraBaseModeParameters();
    public AstraBaseModeParameters astraCar = new AstraBaseModeParameters();
    public AstraBaseModeParameters astraDrt = new AstraBaseModeParameters();

    public class AstraPtParameters {
        public double betaRailTravelTime_u_min = 0.0;
        public double betaBusTravelTime_u_min = 0.0;
        public double betaFeederTravelTime_u_min = 0.0;

        public double betaHeadway_u_min = 0.0;
        public double betaOvgkB_u = 0.0;
        public double betaOvgkC_u = 0.0;
        public double betaOvgkD_u = 0.0;
        public double betaOvgkNone_u = 0.0;
    }

    public AstraPtParameters astraPt = new AstraPtParameters();


    public ZurichDrtParameters drt = new ZurichDrtParameters();

    public double lambdaTravelTimeEuclideanDistance = 0.0;
    public double lambdaCostHouseholdIncome = 0.0;
    public double referenceHouseholdIncome_MU = 0.0;

    public static SwissDrtModeParameters buildASTRA2016() {
        SwissDrtModeParameters parameters = new SwissDrtModeParameters();


        /*// DRT (adapted from public transport)
        parameters.drt.alpha_u = 0.162;
        parameters.drt.betaWaitingTime_u_min = -0.0802;
        parameters.drt.betaTravelTime_u_min = -0.0527;
        parameters.drt.betaAccessEgressTime_u_min = -0.0;
        parameters.astraDrt.betaAgeOver60 = 0.0;
        parameters.astraDrt.betaCity = 0.0;
        parameters.astraDrt.betaWork = 0.0;*/

        // DRT from astra feb2016
        parameters.drt.alpha_u = -0.061;
        parameters.drt.betaWaitingTime_u_min = -0.093;
        parameters.drt.betaTravelTime_u_min = -0.015;
        parameters.drt.betaAccessEgressTime_u_min = -0.0;
        parameters.astraDrt.betaAgeOver60 = -2.6588;
        parameters.astraDrt.betaCity = 0.0;
        parameters.astraDrt.betaWork = -1.938;

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
    public static SwissDrtModeParameters buildAstraFrom6Feb2020() {
        SwissDrtModeParameters parameters = new SwissDrtModeParameters();

        // General
        parameters.betaCost_u_MU = -0.0888;

        parameters.lambdaCostHouseholdIncome = -0.8169;
        parameters.lambdaCostEuclideanDistance = -0.2209;
        parameters.lambdaTravelTimeEuclideanDistance = 0.1147;

        parameters.referenceEuclideanDistance_km = 39.0;
        parameters.referenceHouseholdIncome_MU = 12260.0;
        
        // DRT
        parameters.drt.alpha_u = -0.061;
        parameters.drt.betaWaitingTime_u_min = -0.093;
        parameters.drt.betaTravelTime_u_min = -0.015;
        parameters.drt.betaAccessEgressTime_u_min = -0.0;
        parameters.astraDrt.betaAgeOver60 = -2.6588;
        parameters.astraDrt.betaCity = 0.0;
        parameters.astraDrt.betaWork = -1.938;

        // Public transport
        parameters.pt.alpha_u = 0.0;
        parameters.pt.betaWaitingTime_u_min = -0.0124;
        parameters.pt.betaAccessEgressTime_u_min = -0.0142;

        parameters.astraPt.betaFeederTravelTime_u_min = -0.0452;
        parameters.astraPt.betaBusTravelTime_u_min = -0.0124;
        parameters.astraPt.betaRailTravelTime_u_min = -0.0072;
        parameters.astraPt.betaHeadway_u_min = -0.0301;

        parameters.astraPt.betaOvgkB_u = -1.7436;
        parameters.astraPt.betaOvgkC_u = -1.6413;
        parameters.astraPt.betaOvgkD_u = -0.9649;
        parameters.astraPt.betaOvgkNone_u = -1.0889;

        // Bicycle
        parameters.bike.alpha_u = 0.1522;
        parameters.bike.betaTravelTime_u_min = -0.1258;

        parameters.astraBike.betaAgeOver60 = -2.6588;

        // Car
        parameters.car.alpha_u = -0.8; // Original from fb model: 0.2235;
        parameters.car.betaTravelTime_u_min = -0.0192;

        parameters.astraCar.betaWork = -1.1606;
        parameters.astraCar.betaCity = -0.4590;

        // Walking
        parameters.walk.alpha_u = 0.5903;
        parameters.walk.betaTravelTime_u_min = -0.0457;

        parameters.astraWalk.travelTimeThreshold_min = 120.0;

        return parameters;
    }
}
