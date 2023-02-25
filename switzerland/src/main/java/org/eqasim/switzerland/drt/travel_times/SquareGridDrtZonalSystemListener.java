package org.eqasim.switzerland.drt.travel_times;

import java.util.Collection;

import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;

public class SquareGridDrtZonalSystemListener implements ShutdownListener {

	private SquareGridDrtZonalSystem zones;

	@Inject
	public SquareGridDrtZonalSystemListener(SquareGridDrtZonalSystem zones) {
		this.zones = zones;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String fileName = event.getServices().getControlerIO().getOutputFilename("drt_SquareGridLink2Zones.csv");
		zones.writeLink2Zone(fileName);

		String crs = "EPSG:2056";

		fileName = event.getServices().getControlerIO().getOutputFilename("drt_SquareGridZones.shp");

		Collection<SimpleFeature> features = this.zones.convertGeometriesToSimpleFeatures(crs);
		ShapeFileWriter.writeGeometries(features, fileName);

	}
}
