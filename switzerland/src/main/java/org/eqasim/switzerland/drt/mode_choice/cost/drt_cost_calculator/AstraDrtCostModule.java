package org.eqasim.switzerland.drt.mode_choice.cost.drt_cost_calculator;

import org.eqasim.switzerland.drt.mode_choice.parameters.SwissDrtCostParameters;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtVehicleDistanceStats;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class AstraDrtCostModule extends AbstractDvrpModeModule {

    public AstraDrtCostModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
    }
    @Override
    public void install() {
        // cost calculator
        addControlerListenerBinding().toProvider(
                modalProvider(
                getter -> new DrtCostControlerListener(
                        getter.getModal(DrtVehicleDistanceStats.class),
                        getter.getModal(DrtEventSequenceCollector.class),
                        getter.get(SwissDrtCostParameters.class)
                )
                )).asEagerSingleton();

    }


}
