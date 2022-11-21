package org.eqasim.switzerland.drt.wait_time;

import com.google.common.base.Preconditions;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.switzerland.drt.wait_time.DrtWaitTimes;
import org.eqasim.switzerland.drt.wait_time.WaitTimeTracker;
import org.eqasim.switzerland.drt.wait_time.WayneCountyDrtZonalSystem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;

public class SwissDrtWaitTimeModule extends AbstractEqasimExtension {

    private final DrtConfigGroup drtConfig;
    private final Scenario scenario;

    public SwissDrtWaitTimeModule(DrtConfigGroup drtConfig, Scenario scenario) {
        this.drtConfig = drtConfig;
        this.scenario = scenario;
    }

    @Override
    protected void installEqasimExtension() {

        addEventHandlerBinding().to(WaitTimeTracker.class);

        bind(WaitTimeTracker.class).asEagerSingleton();
        addControlerListenerBinding().to(DrtWaitTimes.class);
        bind(DrtWaitTimes.class).asEagerSingleton();


        DrtZonalSystemParams params = this.drtConfig.getZonalSystemParams().orElseThrow();
        Preconditions.checkNotNull(params.getCellSize());
        WayneCountyDrtZonalSystem drtZonalSystem = new WayneCountyDrtZonalSystem(scenario.getNetwork(),params.getCellSize());
        bind(WayneCountyDrtZonalSystem.class).toInstance(drtZonalSystem);
        
        addControlerListenerBinding().to(DrtWaitTimeGlobal.class);
        bind(DrtWaitTimeGlobal.class).asEagerSingleton();
        
    }
}
