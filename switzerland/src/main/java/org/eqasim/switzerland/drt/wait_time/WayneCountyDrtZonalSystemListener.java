package org.eqasim.switzerland.drt.wait_time;

import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;

public class WayneCountyDrtZonalSystemListener implements ShutdownListener{
	
	private WayneCountyDrtZonalSystem zones;
	
	@Inject
	public WayneCountyDrtZonalSystemListener(WayneCountyDrtZonalSystem zones) {
		this.zones = zones;
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String fileName = event.getServices()
				.getControlerIO()
				.getOutputFilename("drt_WayneCountyLink2Zones.csv");
		zones.writeLink2Zone(fileName);
	}
}
