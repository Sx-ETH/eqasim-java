package org.eqasim.ile_de_france;

import org.matsim.contrib.cba.CbaConfigGroup;
import org.matsim.contrib.cba.CbaModule;
import org.matsim.contrib.cba.analyzers.agentsAnalysis.AgentsAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.drtAnalysis.DrtAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.genericAnalysis.GenericAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.privateVehiclesAnalysis.PrivateVehiclesAnalyzerConfigGroup;
import org.matsim.contrib.cba.analyzers.ptAnalysis.PtAnalyzerConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class CbaUtils {

    public static void adaptConfig(Config config, boolean drt) {

        CbaConfigGroup cbaConfigGroup = new CbaConfigGroup();
        if(drt) {
            DrtAnalyzerConfigGroup drtAnalyzerConfigGroup = new DrtAnalyzerConfigGroup();
            drtAnalyzerConfigGroup.setMode("drt");
            drtAnalyzerConfigGroup.setTripsSheetName("trips_drt");
            drtAnalyzerConfigGroup.setVehiclesSheetName("vehicles_drt");
            cbaConfigGroup.addParameterSet(drtAnalyzerConfigGroup);
        }


        PtAnalyzerConfigGroup ptAnalyzerConfigGroup = new PtAnalyzerConfigGroup();
        ptAnalyzerConfigGroup.setMode("pt");
        ptAnalyzerConfigGroup.setTripsSheetName("trips_pt");
        ptAnalyzerConfigGroup.setVehiclesSheetName("vehicles_pt");

        AgentsAnalyzerConfigGroup agentsAnalyzerConfigGroup = new AgentsAnalyzerConfigGroup();
        agentsAnalyzerConfigGroup.setScoresSheetName("scores");

        PrivateVehiclesAnalyzerConfigGroup privateVehiclesAnalyzerConfigGroup = new PrivateVehiclesAnalyzerConfigGroup();
        privateVehiclesAnalyzerConfigGroup.setMode("car");
        privateVehiclesAnalyzerConfigGroup.setTripsSheetName("trips_pv");
        privateVehiclesAnalyzerConfigGroup.setIgnoredActivityTypes("DrtStay,DrtBusStop");

        GenericAnalyzerConfigGroup passengerAnalyzerConfigGroup = new GenericAnalyzerConfigGroup();
        passengerAnalyzerConfigGroup.setTripsSheetName("trips_car_passenger");
        passengerAnalyzerConfigGroup.setMode("car_passenger");

        GenericAnalyzerConfigGroup walkAnalyzerConfigGroup = new GenericAnalyzerConfigGroup();
        walkAnalyzerConfigGroup.setTripsSheetName("trips_walk");
        walkAnalyzerConfigGroup.setMode("walk");

        GenericAnalyzerConfigGroup bikeAnalyzerConfigGroup = new GenericAnalyzerConfigGroup();
        bikeAnalyzerConfigGroup.setTripsSheetName("trips_bike");
        bikeAnalyzerConfigGroup.setMode("bike");


        cbaConfigGroup.addParameterSet(bikeAnalyzerConfigGroup);
        cbaConfigGroup.addParameterSet(walkAnalyzerConfigGroup);
        cbaConfigGroup.addParameterSet(passengerAnalyzerConfigGroup);
        cbaConfigGroup.addParameterSet(privateVehiclesAnalyzerConfigGroup);
        cbaConfigGroup.addParameterSet(agentsAnalyzerConfigGroup);
        cbaConfigGroup.addParameterSet(ptAnalyzerConfigGroup);

        config.addModule(cbaConfigGroup);
    }

    public static void adaptControler(Controler controler) {
        controler.addOverridingModule(new CbaModule());
    }
}
