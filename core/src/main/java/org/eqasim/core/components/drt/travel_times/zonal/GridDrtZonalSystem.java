package org.eqasim.core.components.drt.travel_times.zonal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

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

}
