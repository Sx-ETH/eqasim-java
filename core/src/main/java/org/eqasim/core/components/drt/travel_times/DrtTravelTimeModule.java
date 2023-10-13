package org.eqasim.core.components.drt.travel_times;

import org.eqasim.core.components.drt.config_group.DrtMetricSmootheningParamSet;
import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.smoothing.Markov;
import org.eqasim.core.components.drt.travel_times.smoothing.MovingWindow;
import org.eqasim.core.components.drt.travel_times.smoothing.Smoothing;
import org.eqasim.core.components.drt.travel_times.smoothing.SuccessiveAverage;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;

public class DrtTravelTimeModule extends AbstractEqasimExtension {

    private final DrtConfigGroup drtConfig;
    private final Scenario scenario;
    private final DrtModeChoiceConfigGroup drtModeChoiceConfigGroup;

    public DrtTravelTimeModule(DrtConfigGroup drtConfig, Scenario scenario, DrtModeChoiceConfigGroup drtModeChoiceConfigGroup) {
        this.drtConfig = drtConfig;
        this.scenario = scenario;
        this.drtModeChoiceConfigGroup = drtModeChoiceConfigGroup;
    }

    @Override
    protected void installEqasimExtension() {

        addEventHandlerBinding().to(DrtTimeTracker.class);

        bind(DrtTimeTracker.class).asEagerSingleton();

        addControlerListenerBinding().to(TravelTimeUpdates.class);
        bind(TravelTimeUpdates.class).asEagerSingleton();

        bind(DrtPredictions.class).asEagerSingleton();

        binder().requestStaticInjection(TravelTimeUpdates.class);

        DrtMetricSmootheningParamSet drtMetricSmootheningParamSet = drtModeChoiceConfigGroup.getDrtMetricSmootheningParamSet();
        Smoothing smoothing;
        switch (drtMetricSmootheningParamSet.getSmootheningType()) {
            case Markov:
                smoothing = new Markov();
                break;
            case MovingAverage:
                smoothing = new MovingWindow(drtMetricSmootheningParamSet.getMovingWindow());
                break;
            case SuccessiveAverage:
                smoothing = new SuccessiveAverage(drtMetricSmootheningParamSet.getMsaWeight());
                break;
            default:
                throw new IllegalArgumentException("Unknown smoothening type: " + drtMetricSmootheningParamSet.getSmootheningType());
        }
        bind(Smoothing.class).toInstance(smoothing);
    }
}
