package org.eqasim.core.components.drt.travel_times.zonal;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class FixedDrtZonalSystem {
    protected Map<Id<Link>, String> link2zone = new HashMap<>();
    protected Network network;
    protected Map<String, PreparedGeometry> zones = new HashMap<>();
    private static final Logger log = LogManager.getLogger(FixedDrtZonalSystem.class);

    public Geometry getZone(String zone) {
        return (Geometry) zones.get(zone);
    }

    public abstract String getZoneForLinkId(Id<Link> linkId);

    public Collection<SimpleFeature> getSimpleFeatures(String targetCoordinateSystem) {
        SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
        try {
            simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
        } catch (IllegalArgumentException e) {
            log.warn("Coordinate reference system \"" + targetCoordinateSystem
                    + "\" is unknown. Please set a crs in config global. Will try to create shapefile anyway");
        }

        simpleFeatureBuilder.setName("drtAnalysisZones");
        // note: column names may not be longer than 10 characters. Otherwise the name
        // is cut after the 10th character and the value is NULL in QGis
        simpleFeatureBuilder.add("the_geom", Polygon.class);
        simpleFeatureBuilder.add("zoneId", String.class);
        simpleFeatureBuilder.add("centerX", Double.class);
        simpleFeatureBuilder.add("centerY", Double.class);

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());

        Collection<SimpleFeature> features = new ArrayList<>();

        for (Map.Entry<String, PreparedGeometry> entry : this.zones.entrySet()) {
            Object[] attributes = new Object[4];
            attributes[0] = entry.getValue().getGeometry();
            attributes[1] = entry.getKey();
            attributes[2] = entry.getValue().getGeometry().getCentroid().getX();
            attributes[3] = entry.getValue().getGeometry().getCentroid().getY();
            try {
                features.add(builder.buildFeature(entry.getKey(), attributes));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }

        return features;
    }

    public Map<String, PreparedGeometry> getZones() {
        return this.zones;
    }

    public void writeLink2Zone(String fileName) {
        String delimiter = ";";
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        log.info("Link2zone size: " + this.link2zone.size());
        int nWrites = 0;
        try {
            writer.append("link_id;zone");
            for (Map.Entry<Id<Link>, String> entry : this.link2zone.entrySet()) {
                writer.append("\n");
                writer.append(Id.writeId(entry.getKey()));
                writer.append(delimiter);
                writer.append(entry.getValue());
                nWrites += 1;

            }
            writer.flush();
            writer.close();
            if (nWrites < this.link2zone.size()) {
                log.warn("There are less writes in the link2zone file than in the map, be careful when using it");
                log.warn("nWrites: " + nWrites);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
