package org.eqasim.switzerland.drt.wait_time;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WaitTimeTracker implements PersonEntersVehicleEventHandler, DrtRequestSubmittedEventHandler, PassengerDroppedOffEventHandler, PassengerRequestRejectedEventHandler {

    private Map<Id<Person>, DrtTripData> submittedRequest;
    private Set<DrtTripData> drtTrips;

    public WaitTimeTracker(){
        this.submittedRequest = new HashMap<>();
        this.drtTrips = new HashSet<>();
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        DrtTripData drtTrip = new DrtTripData();
        drtTrip.startLinkId = event.getFromLinkId();
        drtTrip.startTime = event.getTime();
        drtTrip.unsharedTime = event.getUnsharedRideTime(); //check for better way to capture the detour?
        this.submittedRequest.put(event.getPersonId(), drtTrip);
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getVehicleId().toString().contains("drt") && this.submittedRequest.containsKey(event.getPersonId())) {
            DrtTripData drtTrip = this.submittedRequest.get(event.getPersonId());
            drtTrip.pickUpTime = event.getTime();
            drtTrip.waitTime = drtTrip.pickUpTime - drtTrip.startTime ;
            this.drtTrips.add(drtTrip);
        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        if (event.getMode().equals("drt") && this.submittedRequest.containsKey(event.getPersonId())) {
            //get dropoff time
            //compute arrival time
            //get unshared vehicle time
            //compute detour time
            double dropoffTime;

            DrtTripData drtTrip = this.submittedRequest.get(event.getPersonId());
            double totalTravelTime = event.getTime() - drtTrip.pickUpTime;


            drtTrip.pickUpTime = event.getTime();
            drtTrip.waitTime = drtTrip.pickUpTime - drtTrip.startTime ;
            this.drtTrips.add(drtTrip);
        }
    }

    //for rejected passengers

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        double simEndTime = 24*3600;  //toDo pull from the config
        if (this.submittedRequest.containsKey(event.getPersonId())) {
            DrtTripData drtTrip = this.submittedRequest.get(event.getPersonId());
            drtTrip.rejected = true;
            drtTrip.pickUpTime = simEndTime; //toDo think why this rejection wait time may not be right
            drtTrip.waitTime = drtTrip.startTime - drtTrip.pickUpTime;
            this.drtTrips.add(drtTrip);

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
