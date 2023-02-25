package org.eqasim.switzerland.drt.travel_times;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class HexGridDrtZonalSystem extends GridDrtZonalSystem {
	private static final Logger log = Logger.getLogger(HexGridDrtZonalSystem.class);

	@Inject
	private HexGridDrtZonalSystem(Network network, @Named("gridCellSize") Double hexRadius) {
		log.info("Start creating the hexagon grid");

		this.network = network;

		double hexApothem = hexRadius * Math.sqrt(3) / 2.0;

		double[] boundingbox = NetworkUtils.getBoundingBox(network.getNodes().values());
		log.info("Bounding box: " + boundingbox);

		GeometryFactory gf = new GeometryFactory();
		PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
		this.quadtree = new QuadTree<>(boundingbox[0], boundingbox[1], boundingbox[2], boundingbox[3]);

		zones = new HashMap<>();
		int cell = 0;

		double centroidX = boundingbox[0];
		double centroidY = boundingbox[1];

		while (centroidY + hexRadius < boundingbox[3]) {
			while (centroidX < boundingbox[2]) {
				cell++;
				Coordinate[] ca = createHexCoordsFromCentroid(centroidX, centroidY, hexRadius);
				Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);

				zones.put(cell + "", preparedGeometryFactory.create(polygon));
				centroidX += hexRadius * 3;
			}
			centroidX = boundingbox[0];
			centroidY += hexApothem * 2;
		}

		// We fill the holes between the previous hexagons
		centroidX = boundingbox[0] + 1.5 * hexRadius;
		centroidY = boundingbox[1] + hexApothem;

		while (centroidY + hexRadius < boundingbox[3]) {
			while (centroidX < boundingbox[2]) {
				cell++;
				Coordinate[] ca = createHexCoordsFromCentroid(centroidX, centroidY, hexRadius);
				Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);

				zones.put(cell + "", preparedGeometryFactory.create(polygon));
				centroidX += hexRadius * 3;
			}
			centroidX = boundingbox[0] + 1.5 * hexRadius;
			centroidY += hexApothem * 2;
		}

		for (Entry<String, PreparedGeometry> zone : zones.entrySet()) {

			double x = zone.getValue().getGeometry().getCentroid().getX();
			double y = zone.getValue().getGeometry().getCentroid().getY();

			quadtree.put(x, y, zone);

		}
		log.info("Finished creating the hexagon grid");

	}

	private Coordinate[] createHexCoordsFromCentroid(double centroidX, double centroidY, double radius) {
		double apothem = radius * Math.sqrt(3) / 2.0;

		Coordinate p1 = new Coordinate(centroidX - radius, centroidY);
		Coordinate p2 = new Coordinate(centroidX - radius / 2.0, centroidY + apothem);
		Coordinate p3 = new Coordinate(centroidX + radius / 2.0, centroidY + apothem);
		Coordinate p4 = new Coordinate(centroidX + radius, centroidY);
		Coordinate p5 = new Coordinate(centroidX + radius / 2.0, centroidY - apothem);
		Coordinate p6 = new Coordinate(centroidX - radius / 2.0, centroidY - apothem);

		Coordinate[] ca = { p1, p2, p3, p4, p5, p6, p1 };
		return ca;
	}

}
