package org.eqasim.switzerland.drt.travel_times;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.switzerland.drt.travel_times.wait_time.DrtZonalWaitTimes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;

import com.google.common.base.Preconditions;
import com.google.inject.name.Names;

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

		DrtZonalSystemParams params = this.drtConfig.getZonalSystemParams().orElseThrow();
		Preconditions.checkNotNull(params.getCellSize());
		Double cellSize = params.getCellSize();
		bind(Double.class).annotatedWith(Names.named("gridCellSize")).toInstance(cellSize);
		bind(SquareGridDrtZonalSystem.class).asEagerSingleton();
		bind(HexGridDrtZonalSystem.class).asEagerSingleton();

		addControlerListenerBinding().to(TravelTimeUpdates.class);
		bind(TravelTimeUpdates.class).asEagerSingleton();

		addControlerListenerBinding().to(SquareGridDrtZonalSystemListener.class);
		bind(SquareGridDrtZonalSystemListener.class).asEagerSingleton();
		
		addControlerListenerBinding().to(HexGridDrtZonalSystemListener.class);
		bind(HexGridDrtZonalSystemListener.class).asEagerSingleton();

	}
}
