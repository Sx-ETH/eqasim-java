package org.eqasim.switzerland.drt.travel_times.zonal;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class SingleZoneDrtZonalSystem extends FixedDrtZonalSystem {
    public SingleZoneDrtZonalSystem(Network network) {
        this.network = network;
        GeometryFactory gf = new GeometryFactory();
        PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
        double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
        Coordinate p1 = new Coordinate(boundingBox[0], boundingBox[1]);
        Coordinate p2 = new Coordinate(boundingBox[2], boundingBox[1]);
        Coordinate p3 = new Coordinate(boundingBox[2], boundingBox[3]);
        Coordinate p4 = new Coordinate(boundingBox[0], boundingBox[3]);
        Coordinate[] ca = {p1, p2, p3, p4, p1};
        Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);
        this.zones.put("global", preparedGeometryFactory.create(polygon));
    }

    @Override
    public String getZoneForLinkId(Id<Link> linkId) {
        link2zone.put(linkId, "global");
        return "global";
    }

}
