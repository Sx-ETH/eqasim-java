package org.eqasim.switzerland.drt.travel_times.zonal;

import com.google.inject.Inject;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

public class HexGridDrtZonalSystemListener implements ShutdownListener {

    private HexGridDrtZonalSystem zones;

    @Inject
    public HexGridDrtZonalSystemListener(HexGridDrtZonalSystem zones) {
        this.zones = zones;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        String fileName = event.getServices().getControlerIO().getOutputFilename("drt_HexGridLink2Zones.csv");
        zones.writeLink2Zone(fileName);

        String crs = "EPSG:2056";

        fileName = event.getServices().getControlerIO().getOutputFilename("drt_HexGridZones.shp");

        Collection<SimpleFeature> features = this.zones.convertGeometriesToSimpleFeatures(crs);
        ShapeFileWriter.writeGeometries(features, fileName);
    }

}
