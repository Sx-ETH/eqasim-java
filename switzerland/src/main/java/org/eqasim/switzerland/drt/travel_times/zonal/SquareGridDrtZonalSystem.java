package org.eqasim.switzerland.drt.travel_times.zonal;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import java.util.Map.Entry;


public class SquareGridDrtZonalSystem extends GridDrtZonalSystem {
    private static final Logger log = Logger.getLogger(SquareGridDrtZonalSystem.class);

    public SquareGridDrtZonalSystem(Network network, double cellSize) {
        log.info("Start creating the square grid");
        this.network = network;
        this.zones = DrtGridUtils.createGridFromNetwork(network, cellSize);

        // build a quadtree for the zones in the network with their centroid
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        this.quadtree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        for (Entry<String, PreparedGeometry> zone : zones.entrySet()) {

            double x = zone.getValue().getGeometry().getCentroid().getX();
            ;
            double y = zone.getValue().getGeometry().getCentroid().getY();
            ;

            // if(x < minX || x > maxX || y > maxY || y < minY)
            if (!(x < bounds[0] || y < bounds[1] || x > bounds[2] || y > bounds[3])) {
                this.quadtree.put(x, y, zone);
            }
        }
        log.info("Finished creating the square grid");

    }

}
