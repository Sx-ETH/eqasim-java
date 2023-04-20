package org.eqasim.switzerland.drt.travel_times.zonal;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.Map;

public class TAZDrtZonalSystem extends FixedDrtZonalSystem {
    private ShapeFileReader shapeFileReader;
    private Collection<SimpleFeature> features;


    public TAZDrtZonalSystem(Network network, String filename) {
        this.shapeFileReader = new ShapeFileReader();
        this.features = this.shapeFileReader.readFileAndInitialize(filename);
        this.network = network;
        for (SimpleFeature feature : features) {
            String zoneId = feature.getID();
            System.out.println(zoneId);

            for (Property p : feature.getProperties()) {
                System.out.println(p.getName() + " " + p.getValue());
            }
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(geometry);
            this.zones.put(zoneId, preparedGeometry);
        }
    }

    @Override
    public String getZoneForLinkId(Id<Link> linkId) {
        if (this.link2zone.containsKey(linkId)) {
            return link2zone.get(linkId);
        }
        Coord coord = network.getLinks().get(linkId).getCoord();
        for (Map.Entry<String, PreparedGeometry> entry : this.zones.entrySet()) {
            if (entry.getValue().contains(MGC.coord2Point(coord))) {
                link2zone.put(linkId, entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Collection<SimpleFeature> getSimpleFeatures(String crs) {
        return this.features;
    }


}
