package org.eqasim.switzerland.drt.travel_times;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.switzerland.drt.mode_choice.DrtDistanceConstraint;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtZonalWaitTimes;
import org.matsim.api.core.v01.Scenario;
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
        addControlerListenerBinding().to(DrtZonalWaitTimes.class);
        bind(DrtZonalWaitTimes.class).asEagerSingleton();

        addControlerListenerBinding().to(TravelTimeUpdates.class);
        bind(TravelTimeUpdates.class).asEagerSingleton();

        bind(DrtPredictions.class).asEagerSingleton();

        binder().requestStaticInjection(DrtGlobalMetrics.class);
        binder().requestStaticInjection(TravelTimeUpdates.class);
        bindTripConstraintFactory(DrtDistanceConstraint.NAME).to(DrtDistanceConstraint.Factory.class);
    }
}
