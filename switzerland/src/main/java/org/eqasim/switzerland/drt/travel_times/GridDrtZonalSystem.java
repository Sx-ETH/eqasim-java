package org.eqasim.switzerland.drt.travel_times;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

public abstract class GridDrtZonalSystem implements FixedDrtZonalSystem {
	protected Map<Id<Link>, String> link2zone = new HashMap<>();;
	protected Network network = null;
	protected Map<String, PreparedGeometry> zones = null;
	protected QuadTree<Entry<String, PreparedGeometry>> quadtree = null;
	private static final Logger log = LogManager.getLogger(GridDrtZonalSystem.class);

	@Override
	public Geometry getZone(String zone) {
		return (Geometry) zones.get(zone);
	}

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

	@Override
	public Map<String, PreparedGeometry> getZones() {
		return this.zones;
	}

	@Override
	public void writeLink2Zone(String fileName) {
		String delimiter = ";";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		log.info("Link2zone size: " + this.link2zone.size());
		int nWrites = 0;
		try {
			writer.append("link_id;zone");
			for (Entry<Id<Link>, String> entry : this.link2zone.entrySet()) {
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
