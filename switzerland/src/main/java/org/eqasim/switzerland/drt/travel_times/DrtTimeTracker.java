package org.eqasim.switzerland.drt.travel_times;


import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

import java.util.*;

public class DrtTimeTracker implements PassengerPickedUpEventHandler, DrtRequestSubmittedEventHandler, PassengerDroppedOffEventHandler, PassengerRequestRejectedEventHandler {

    private Map<Id<Request>, DrtTripData> submittedRequest;
    private Set<DrtTripData> drtTrips;
    private final TripRouter tripRouter;

    private final Scenario scenario;

    @Inject
    public DrtTimeTracker(TripRouter tripRouter, Scenario scenario) {
        this.scenario = scenario;
        this.submittedRequest = new HashMap<>();
        this.drtTrips = new HashSet<>();
        this.tripRouter = tripRouter;
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        DrtTripData drtTrip = new DrtTripData();
        //ToDo need to check why I have to cast here
        drtTrip.requestId = event.getRequestId();
        drtTrip.personId = event.getPersonId();
        drtTrip.startTime = event.getTime();
        drtTrip.estimatedUnsharedTime = event.getUnsharedRideTime();
        drtTrip.unsharedDistance = event.getUnsharedRideDistance();
        drtTrip.startLinkId = event.getFromLinkId();
        drtTrip.endLinkId = event.getToLinkId();
        this.submittedRequest.put(event.getRequestId(), drtTrip);
    }

    //will use DrtPickedup event rather than personEntersVehicle
    // because in a tour case the person id as the key would not work
    @Override
    public void handleEvent(PassengerPickedUpEvent event) {
        if (event.getMode().equals("drt") && this.submittedRequest.containsKey(event.getRequestId())) {
            DrtTripData drtTrip = this.submittedRequest.get(event.getRequestId());
            drtTrip.pickUpTime = event.getTime();
            drtTrip.waitTime = drtTrip.pickUpTime - drtTrip.startTime;

        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {

        //Here we collect detour/delay related information to compute a delay factor
        if (event.getMode().equals("drt") && this.submittedRequest.containsKey(event.getRequestId())) {
            DrtTripData drtTrip = this.submittedRequest.get(event.getRequestId());
            drtTrip.totalTravelTime = event.getTime() - drtTrip.pickUpTime;
            drtTrip.arrivalTime = event.getTime();
            //compute unshared rideTime from current route

            //use injected trip router
            Link fromLink = scenario.getNetwork().getLinks().get(drtTrip.startLinkId);
            Link toLink = scenario.getNetwork().getLinks().get(drtTrip.endLinkId);
            Person person = scenario.getPopulation().getPersons().get(event.getPersonId());


            //update start and end coordinates for later use
            drtTrip.startCoord = fromLink.getCoord();
            drtTrip.endCoord = toLink.getCoord();

            //create route using trip router to get possible travel time for unshared trip in current iteration
            List<? extends PlanElement> routeElements = this.tripRouter.calcRoute("car", new LinkWrapperFacility(fromLink), new LinkWrapperFacility(toLink), drtTrip.pickUpTime, person, null);

            for (Leg leg : TripStructureUtils.getLegs(routeElements)) {
                if (leg.getMode().equals("car")) {
                    drtTrip.routerUnsharedTime = leg.getTravelTime().seconds();
                }
            }

            this.drtTrips.add(drtTrip);
        }
    }

    //for rejected passengers

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        double simEndTime = 24 * 3600;  //toDo pull from the config
        if (this.submittedRequest.containsKey(event.getRequestId())) {
            DrtTripData drtTrip = this.submittedRequest.get(event.getRequestId());
            drtTrip.rejected = true;
            drtTrip.pickUpTime = simEndTime; //toDo think why this rejection wait time may not be right
            drtTrip.waitTime = drtTrip.startTime - drtTrip.pickUpTime;
            //this.drtTrips.add(drtTrip);

        }

    }

    //When we work with zone averages rejected trips influences these averages a lot and a zone with one single rejection
    // that happened early in the day could have a huge effect on the wait time of that zone at that time. Maybe there is
    // need to compute a probability for rejections that can be used to weight the wait time
    //To be fair if rejections is allowed then wait times at the zones would not be influenced by the rejections rather
    //rejections are influenced by the wait times and should we then consider them here?
    // Maybe treat the rejection effects separately?

    @Override
    public void reset(int iteration) {
        //clear submitted request and drtTrips makes it provide values per iteration
        this.submittedRequest.clear();
        this.drtTrips.clear();
    }

    public Set<DrtTripData> getDrtTrips() {

        return drtTrips;
    }

    public void setDrtTrips(Set<DrtTripData> newDrtTrips) {

        this.drtTrips = newDrtTrips;
    }

}
