package org.eqasim.switzerland.drt.mode_choice.cost.drt_cost_calculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtCostParameters;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtVehicleDistanceStats;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

import javax.inject.Inject;

public class AstraDrtCostModule extends AbstractDvrpModeModule {

    public AstraDrtCostModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
    }
    @Override
    public void install() {
        // cost calculator
        addControlerListenerBinding().toProvider(
                modalProvider(
                getter -> new DrtCostControlerListener(getter.getModal(DrtVehicleDistanceStats.class), getter.getModal(DrtEventSequenceCollector.class))
                )).asEagerSingleton();

    }


}
