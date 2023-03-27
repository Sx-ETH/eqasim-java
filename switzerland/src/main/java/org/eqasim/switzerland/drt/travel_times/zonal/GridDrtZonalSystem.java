package org.eqasim.switzerland.drt.travel_times.zonal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public abstract class GridDrtZonalSystem extends FixedDrtZonalSystem {
    protected QuadTree<Entry<String, PreparedGeometry>> quadtree;
    private static final Logger log = LogManager.getLogger(GridDrtZonalSystem.class);


    @Override
    public String getZoneForLinkId(Id<Link> linkId) {
        if (this.link2zone.containsKey(linkId)) {
            return link2zone.get(linkId);
        }

        // get the nearest zone centroid to this linkId
        Coord coord = network.getLinks().get(linkId).getCoord();
        String zoneId = quadtree.getClosest(coord.getX(), coord.getY()).getKey();
        if (zoneId == null) {
            link2zone.put(linkId, null);
            return null;
        }
        link2zone.put(linkId, zoneId);
        return zoneId;
    }


    public Collection<SimpleFeature> convertGeometriesToSimpleFeatures(String targetCoordinateSystem) {
        SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
        try {
            simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
        } catch (IllegalArgumentException e) {
            log.warn("Coordinate reference system \"" + targetCoordinateSystem
                    + "\" is unknown. Please set a crs in config global. Will try to create drt_hexZones.shp anyway");
        }

        simpleFeatureBuilder.setName("drtZoneFeature");
        // note: column names may not be longer than 10 characters. Otherwise the name
        // is cut after the 10th character and the value is NULL in QGis
        simpleFeatureBuilder.add("the_geom", Polygon.class);
        simpleFeatureBuilder.add("zoneIid", String.class);
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

}
