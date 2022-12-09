package org.eqasim.switzerland.drt.TravelTimes.wait_time;

import com.google.common.base.Preconditions;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.switzerland.drt.TravelTimes.detour_time.DrtDetourTimes;
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

        addEventHandlerBinding().to(DrtTimeTracker.class);

        bind(DrtTimeTracker.class).asEagerSingleton();
        addControlerListenerBinding().to(DrtWaitTimes.class);
        bind(DrtWaitTimes.class).asEagerSingleton();
        addControlerListenerBinding().to(DrtDetourTimes.class);
        bind(DrtDetourTimes.class).asEagerSingleton();


        DrtZonalSystemParams params = this.drtConfig.getZonalSystemParams().orElseThrow();
        Preconditions.checkNotNull(params.getCellSize());
        WayneCountyDrtZonalSystem drtZonalSystem = new WayneCountyDrtZonalSystem(scenario.getNetwork(),params.getCellSize());
        bind(WayneCountyDrtZonalSystem.class).toInstance(drtZonalSystem);
    }
}
