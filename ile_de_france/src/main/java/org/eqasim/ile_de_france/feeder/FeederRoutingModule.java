package org.eqasim.ile_de_france.feeder;

import org.apache.log4j.Logger;
import org.eqasim.ile_de_france.drt.mode_choice.IDFDrtModeAvailability;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.CarModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;



public class FeederRoutingModule implements RoutingModule {
	private final RoutingModule drtRoutingModule;
	private final RoutingModule transitRoutingModule;

	private final PopulationFactory populationFactory;

	private static final Logger logger = Logger.getLogger(FeederRoutingModule.class);

	private final QuadTree<Facility> quadTree;

	public  FeederRoutingModule(RoutingModule feederRoutingModule, RoutingModule transitRoutingModule,
							   PopulationFactory populationFactory, TransitSchedule schedule, Network drtNetwork) {
		this.drtRoutingModule = feederRoutingModule;
		this.transitRoutingModule = transitRoutingModule;
		this.populationFactory = populationFactory;
		double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
		quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()){
				if(transitRoute.getTransportMode().equals("rail") || transitRoute.getTransportMode().equals("subway")) {
					for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
						TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
						if(!processedFacilities.contains(transitStopFacility.getId())) {
							processedFacilities.add(transitStopFacility.getId());
							Facility interactionFacility = FacilitiesUtils.wrapLink(NetworkUtils.getNearestLink(drtNetwork, transitStopFacility.getCoord()));
							try {
								if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), interactionFacility)) {
									System.out.println("Cannot add this stop : " + transitStopFacility.getName());
								}
							} catch (IllegalArgumentException exception) {
								System.out.println("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
							}
						}
					}
				}
			}
		}
	}

	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		// Identify closest stations from the origin and destination of the trip
		Facility accessFacility = this.quadTree.getClosest(fromFacility.getCoord().getX(), fromFacility.getCoord().getY());
		Facility egressFacility = this.quadTree.getClosest(toFacility.getCoord().getX(), toFacility.getCoord().getY());

		List<PlanElement> intermodalRoute = new LinkedList<>();
		// Computing the access DRT route
		List<? extends PlanElement> drtRoute = null;
		// If the trip starts right after an outside activity, we leave its first part as PT
		if(! (fromFacility instanceof ActivityFacilityImpl) || ! ((ActivityFacilityImpl) fromFacility).getId().toString().startsWith("outside")) {
			drtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, accessFacility, departureTime, person));
		}
		double accessTime = departureTime;
		if(drtRoute == null) {
			// if no DRT route, next part of the trip starts from the origin
			accessFacility = fromFacility;
		}
		else {
			//Otherwise we have already a first part of the trip
			intermodalRoute.addAll(drtRoute);
			for (PlanElement element : intermodalRoute) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;
					accessTime = Math.max(accessTime, leg.getDepartureTime().seconds());
					accessTime += leg.getTravelTime().seconds();
				}
			}
			Activity accessInteractionActivity = populationFactory.createActivityFromLinkId("feeder interaction", accessFacility.getLinkId());
			accessInteractionActivity.setMaximumDuration(0);
			intermodalRoute.add(accessInteractionActivity);
		}

		// Compute the PT part of the route
		List<PlanElement> ptRoute = new LinkedList<>(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, egressFacility, accessTime, person)));
		double egressTime = accessTime;

		for (PlanElement element : ptRoute) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				egressTime = Math.max(egressTime, leg.getDepartureTime().seconds());
				egressTime += leg.getTravelTime().seconds();
			}
		}

		// Compute the egress DRT route
		// Same as above, if the trip ends righe before an outside activity, we leave its last part as PT
		if(!(egressFacility instanceof ActivityFacilityImpl) || ! ((ActivityFacilityImpl) egressFacility).getId().toString().startsWith("outside"))
		{
			drtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(egressFacility, toFacility, egressTime, person));
		}
		else
		{
			drtRoute = null;
		}

		// If no valid DRT route is found, we recompute a PT route from the access facility to the trip destination
		if(drtRoute == null) {
			intermodalRoute.addAll(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, toFacility, accessTime, person)));
		} else {
			// Otherwise we add it as an egress to the whole route
			intermodalRoute.addAll(ptRoute);
			Activity egressInteractionActivity = populationFactory.createActivityFromLinkId("feeder interaction", egressFacility.getLinkId());
			egressInteractionActivity.setMaximumDuration(0);
			intermodalRoute.add(egressInteractionActivity);
			intermodalRoute.addAll(drtRoute);
		}
		return intermodalRoute;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
		return this.calcRoute(routingRequest.getFromFacility(), routingRequest.getToFacility(), routingRequest.getDepartureTime(), routingRequest.getPerson());
	}
}
