package org.eqasim.core.components.drt.travel_times.dynamic;

import org.eqasim.core.components.drt.config_group.DrtDynamicSystemParamSet;
import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.eqasim.core.components.drt.travel_times.DataStats;
import org.eqasim.core.components.drt.travel_times.DrtTimeUtils;
import org.eqasim.core.components.drt.travel_times.DrtTripData;
import org.eqasim.core.components.drt.utils.QuadTree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.network.NetworkUtils;

import java.util.*;

import static org.eqasim.core.components.drt.travel_times.zonal.DrtFixedZoneMetrics.collectDistances;
import static org.eqasim.core.components.drt.travel_times.zonal.DrtFixedZoneMetrics.collectWaitTimes;

public class DynamicWaitTimeMetrics {

    private List<QuadTree<DrtTripData>> quadTreesByTimeBins = null;
    private final Network network;
    private final DrtTimeUtils timeUtils;

    public DynamicWaitTimeMetrics(Network network, DrtTimeUtils timeUtils) {
        this.network = network;
        this.timeUtils = timeUtils;
    }

    public void prepareDynamicLocationsAndTimeBins(Set<DrtTripData> drtTrips) {

        int nTimeBins = this.timeUtils.getBinCount();

        //get the bounding box for the quadtrees
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());

        //generate a list for quadtrees based on the timebin
        quadTreesByTimeBins = new ArrayList<>(nTimeBins);

        for (int i = 0; i < nTimeBins; i++) {
            //create quadtree per timebin

            quadTreesByTimeBins.add(new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        for (DrtTripData drtTrip : drtTrips) {
            int timeBin = this.timeUtils.getBinIndex(drtTrip.startTime);

            //get the quadtree from the time slot and add the corresponding drt trip
            quadTreesByTimeBins.get(timeBin).put(drtTrip.startCoord.getX(), drtTrip.startCoord.getY(), drtTrip);

            //Test it got put inside
            //System.out.println(quadTreesByTimeBins.get(timeBin));
            //should we get link coords instead of the start?
        }
    }

    public double getDynamicWaitTimeForTimeBin(DrtRoute route, int timeBin, DrtDynamicSystemParamSet.Type dynamicType, int kValue, double radius, double kShare, int kMax, DrtModeChoiceConfigGroup.Feedback feedback, DrtDynamicSystemParamSet.DecayType decayType) {
        if (timeBin >= quadTreesByTimeBins.size()) {
            return Double.NaN;
        }
        //get the coordinates of the route
        Coord startLocation = network.getLinks().get(route.getStartLinkId()).getCoord();
        QuadTree<DrtTripData> quadTree = quadTreesByTimeBins.get(timeBin);
        Collection<DrtTripData> waitTimes;

        if (quadTree.size() == 0) {
            return Double.NaN;
        }
        switch (dynamicType) {
            case KNN_CN:
                waitTimes = quadTree.getKNearestNeighbors(startLocation.getX(), startLocation.getY(), kValue);
                break;
            case KNN_PN:
                //get share of data in timebin to get kValue
                kValue = (int) Math.min(kMax, Math.ceil(quadTree.size() * kShare));
                waitTimes = quadTree.getKNearestNeighbors(startLocation.getX(), startLocation.getY(), kValue);
                break;
            case FD:
                waitTimes = quadTree.getDisk(startLocation.getX(), startLocation.getY(), radius);
                break;
            default:
                throw new RuntimeException(dynamicType + " as a dynamicType not valid, the options are KNN_CN, KNN_PN,fixedDistance");
        }
        Set<DrtTripData> waitTimesSet = new HashSet<>(waitTimes);
        //DataStats(double[] stats, double[] distances, DrtDynamicSystemParamSet.DecayType decayType)
        DataStats waitTimeStats = new DataStats(collectWaitTimes(waitTimesSet), collectDistances(waitTimesSet, startLocation), decayType);
        return waitTimeStats.getStat(feedback);

    }

}
