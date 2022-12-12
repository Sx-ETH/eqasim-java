package org.eqasim.switzerland.drt.travel_times;

import com.google.common.base.Preconditions;
import com.google.inject.name.Names;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.switzerland.drt.travel_times.detour_time.DrtDetourTimes;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtWaitTimeGlobal;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtWaitTimes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;

public class SwissDrtTravelTimeModule extends AbstractEqasimExtension {

    private final DrtConfigGroup drtConfig;
    private final Scenario scenario;

    public SwissDrtTravelTimeModule(DrtConfigGroup drtConfig, Scenario scenario) {
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
        Double cellSize = params.getCellSize();
        bind(Double.class).annotatedWith(Names.named("gridCellSize")).toInstance(cellSize);
        bind(WayneCountyDrtZonalSystem.class).asEagerSingleton();
        
        addControlerListenerBinding().to(DrtWaitTimeGlobal.class);
        bind(DrtWaitTimeGlobal.class).asEagerSingleton();
        
        addControlerListenerBinding().to(WayneCountyDrtZonalSystemListener.class);
        bind(WayneCountyDrtZonalSystemListener.class).asEagerSingleton();
        
    }
}
