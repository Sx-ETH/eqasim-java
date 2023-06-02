package org.eqasim.core.components.drt.mode_choice.utilities;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.ModeChoicePerformedEvent;
import org.matsim.contribs.discrete_mode_choice.model.ModeChoicePerformedEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrtVariablesDeltaRecorder implements PassengerDroppedOffEventHandler, DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, ModeChoicePerformedEventHandler, IterationEndsListener, IterationStartsListener {

    private final Map<Id<Person>, Double> lastDrtRequestSubmissionTime = new HashMap<>();
    //TODO Replace Maps by IdMaps
    private final IdMap<Person, Double> myIdMap = new IdMap<>(Person.class);

    private final Map<Id<Person>, Double> lastPickedUpTime = new HashMap<>();

    private final Map<Id<Person>, List<DrtVariablesEstimationRecord>> cache = new HashMap<>();

    private final Map<Id<Person>, Integer> currentIndices = new HashMap<>();

    private final Map<Id<Person>, Map<Integer, DrtVariables>> observed = new HashMap<>();

    private final MatsimServices matsimServices;

    private boolean cacheFilled = false;

    private int drtVariablesComputations = 0;
    private int modeChoices = 0;

    private final DrtPredictorInterface drtPredictor;
    private final EventsManager eventsManager;

    @Inject
    public DrtVariablesDeltaRecorder(MatsimServices matsimServices, DrtPredictorInterface drtPredictor, EventsManager eventsManager) {
        this.matsimServices = matsimServices;
        this.drtPredictor = drtPredictor;
        this.eventsManager = eventsManager;
    }

    private void initObservationEntry(Id<Person> personId) {
        if(!this.observed.containsKey(personId)) {
            this.observed.put(personId, new HashMap<>());
        }
    }

    private DrtVariables getCurrentEstimation(Id<Person> personId) {
        return this.observed.get(personId).get(this.currentIndices.get(personId));
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {
        /*this.lastDrtRequestSubmissionTime.put(drtRequestSubmittedEvent.getPersonId(), drtRequestSubmittedEvent.getTime());
        if(this.cacheFilled) {
            this.initObservationEntry(drtRequestSubmittedEvent.getPersonId());
            this.initNextTripEstimation(drtRequestSubmittedEvent.getPersonId());
        }*/
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent passengerPickedUpEvent) {
        /*if(!this.cacheFilled) {
            return;
        }
        DrtVariables drtVariables = this.getCurrentEstimation(passengerPickedUpEvent.getPersonId());
        double waitingTime = passengerPickedUpEvent.getTime() - this.lastDrtRequestSubmissionTime.get(passengerPickedUpEvent.getPersonId());
        DrtVariables newVariables = new DrtVariables(drtVariables.travelTime_min, drtVariables.cost_MU, drtVariables.euclideanDistance_km, waitingTime, drtVariables.accessEgressTime_min);
        this.updateCurrentEstimation(passengerPickedUpEvent.getPersonId(), newVariables);
        this.lastPickedUpTime.put(passengerPickedUpEvent.getPersonId(), passengerPickedUpEvent.getTime());*/
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent passengerDroppedOffEvent) {
        /*if(!this.cacheFilled) {
            return;
        }
        double travelTime = passengerDroppedOffEvent.getTime() - this.lastPickedUpTime.get(passengerDroppedOffEvent.getPersonId());
        DrtVariables drtVariables = this.getCurrentEstimation(passengerDroppedOffEvent.getPersonId());
        DrtVariables newVariables = new DrtVariables(travelTime, drtVariables.cost_MU, drtVariables.euclideanDistance_km, drtVariables.waitingTime_min, drtVariables.accessEgressTime_min);
        this.updateCurrentEstimation(passengerDroppedOffEvent.getPersonId(), newVariables);*/
    }


    @Override
    public void reset(int iteration) {
        this.cache.clear();

        /*
        if(!this.cacheFilled) {
            return;
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.matsimServices.getControlerIO().getIterationFilename(iteration-1, "drtEstimatorDelta.csv"))));
            String delimiter = ";";
            String header = String.join(delimiter, new String[] {
                    "personId",
                    "tripIndex",
                    "observedTravelTime",
                    "observedWaitingTime",
                    "estimatedTravelTime",
                    "estimatedWaitingTime"
            });
            writer.write(header + "\n");
            writer.flush();

            for(Id<Person> personId : this.observed.keySet()) {
                for(Integer tripIndex : this.observed.get(personId).keySet()) {
                    DrtVariables observedVariables = this.observed.get(personId).get(tripIndex);
                    DrtVariables estimatedVariables = this.cache.get(personId).get(tripIndex);
                    String row = String.join(delimiter, new String[] {
                            personId.toString(),
                            tripIndex + "",
                            observedVariables.travelTime_min + "",
                            observedVariables.waitingTime_min + "",
                            estimatedVariables.travelTime_min + "",
                            estimatedVariables.waitingTime_min + ""
                    });
                    writer.write(row + "\n");
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.lastDrtRequestSubmissionTime.clear();
        this.lastPickedUpTime.clear();
        this.observed.clear();*/
    }


    @Override
    public void handleEvent(ModeChoicePerformedEvent event) {
        List<TripCandidate> result = event.getModeChoiceResult();
        List<DiscreteModeChoiceTrip> trips = event.getTrips();
        assert result.size() == trips.size();
        for(int i=0; i<trips.size(); i++) {
            TripCandidate tripCandidate = result.get(i);
            if(tripCandidate.getMode().equals("drt")) {
                assert tripCandidate instanceof DefaultRoutedTripCandidate;
                DefaultRoutedTripCandidate routedTripCandidate = (DefaultRoutedTripCandidate) tripCandidate;
                DrtVariables drtVariables = this.drtPredictor.predict(event.getPerson(), trips.get(i), routedTripCandidate.getRoutedPlanElements());
                if(!this.cache.containsKey(event.getPerson().getId())) {
                    this.cache.put(event.getPerson().getId(), new ArrayList<>());
                }
                this.cache.get(event.getPerson().getId()).add(new DrtVariablesEstimationRecord(routedTripCandidate, drtVariables));
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        this.eventsManager.removeHandler(this);
        this.cache.clear();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        this.eventsManager.addHandler(this);
    }


    public static class DrtVariablesEstimationRecord {

        private final DefaultRoutedTripCandidate routedTripCandidate;
        private final DrtVariables drtVariables;

        public DrtVariablesEstimationRecord(DefaultRoutedTripCandidate routedTripCandidate, DrtVariables drtVariables) {
            this.routedTripCandidate = routedTripCandidate;
            this.drtVariables = drtVariables;
        }

        public DefaultRoutedTripCandidate getRoutedTripCandidate() {
            return this.routedTripCandidate;
        }

        public DrtVariables getDrtVariables() {
            return this.drtVariables;
        }
    }
}
