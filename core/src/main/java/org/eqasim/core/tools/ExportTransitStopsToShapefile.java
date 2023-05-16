package org.eqasim.core.tools;

import java.util.*;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportTransitStopsToShapefile {
	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "output-path", "crs")
				.allowOptions("modes", "transit-lines", "transit-routes")//
				.build();

		String schedulePath = cmd.getOptionStrict("schedule-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(schedulePath);

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));

		Collection<SimpleFeature> features = new LinkedList<>();

		PointFeatureFactory pointFactory = new PointFeatureFactory.Builder() //
				.setCrs(crs).setName("id") //
				.addAttribute("id", String.class) //
				.addAttribute("link", String.class) //
				.create();

		Collection<TransitStopFacility> facilities;
		if(cmd.hasOption("transit-routes")) {
			facilities = new HashSet<>();
			List<Id<TransitRoute>> transitRoutesIds = new ArrayList<>();
			for(String transitRouteId : cmd.getOptionStrict("transit-routes").split(",")) {
				transitRoutesIds.add(Id.create(transitRouteId, TransitRoute.class));
			}
			for(TransitLine transitLine: scenario.getTransitSchedule().getTransitLines().values()) {
				for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
					if(!transitRoutesIds.contains(transitRoute.getId())) {
						continue;
					}
					for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
						facilities.add(transitRouteStop.getStopFacility());
					}
				}
			}
		}
		else {
			facilities = scenario.getTransitSchedule().getFacilities().values();
		}
		for (TransitStopFacility stopFacility : facilities) {
			Coordinate coordinate = new Coordinate(stopFacility.getCoord().getX(), stopFacility.getCoord().getY());

			SimpleFeature feature = pointFactory.createPoint( //
					coordinate, //
					new Object[] { //
							stopFacility.getId().toString(), //
							stopFacility.getLinkId().toString() //
					}, null);

			features.add(feature);
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}