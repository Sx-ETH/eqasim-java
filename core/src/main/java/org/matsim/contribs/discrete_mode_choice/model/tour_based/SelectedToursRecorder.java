package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourSelectorEvent;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourSelectorEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectedToursRecorder extends MatsimXmlWriter implements TourSelectorEventHandler, IterationStartsListener, BeforeMobsimListener {

    private List<TourSelectorEvent> tourSelectorEvents = new ArrayList<>();
    private final MatsimServices matsimServices;
    private final EventsManager eventsManager;

    @Inject
    public SelectedToursRecorder(MatsimServices matsimServices, EventsManager eventsManager) {
        this.matsimServices = matsimServices;
        this.eventsManager = eventsManager;
    }

    @Override
    public void handleEvent(TourSelectorEvent tourSelectorEvent) {
        this.tourSelectorEvents.add(tourSelectorEvent);
    }


    private void write(String filePath){
        openFile(filePath);
        writeStartTag("TourSelectorEvents", Collections.emptyList());
        this.tourSelectorEvents.forEach(this::writeTourSelectorEvent);
        writeEndTag("TourSelectorEvents");
        close();
    }

    private void writeTourSelectorEvent(TourSelectorEvent tourSelectorEvent) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("person", tourSelectorEvent.getPerson().getId().toString()));
        attributes.add(Tuple.of("status", tourSelectorEvent.getStatus()));
        this.writeStartTag(TourSelectorEvent.EVENT_TYPE, attributes);
        this.writeTourCandidate(tourSelectorEvent.getSelected(), "selected");
        for(TourCandidate tourCandidate: tourSelectorEvent.getCandidates()) {
            if(tourCandidate.equals(tourSelectorEvent.getSelected())){
                continue;
            }
            this.writeTourCandidate(tourCandidate, "notSelected");
        }
        for(TourCandidate tourCandidate: tourSelectorEvent.getCandidatesExcludedAfterEstimation()) {
            this.writeTourCandidate(tourCandidate, "excludedAfterEstimation");
        }
        for(List<String> tourModes: tourSelectorEvent.getTourModesExcludedBeforeEstimation()) {
            this.writeTourCandidate(this.tourModesToCandidate(tourModes), "excludedBeforeEstimation");
        }
        this.writeEndTag(TourSelectorEvent.EVENT_TYPE);
    }

    private TourCandidate tourModesToCandidate(List<String> tourModes) {
        List<TripCandidate> tripCandidates = new ArrayList<>();
        for(String mode: tourModes) {
            tripCandidates.add(new DefaultTripCandidate(0, mode, 0));
        }
        return new DefaultTourCandidate(0, tripCandidates);
    }

    private void writeTourCandidate(TourCandidate tourCandidate, String status) {
        List<Tuple<String, String>> attributes = new ArrayList<>();

        attributes.add(Tuple.of("status", status));

        attributes.add(Tuple.of("utility", ""+tourCandidate.getUtility()));
        writeStartTag("TourCandidate", attributes);
        tourCandidate.getTripCandidates().forEach(this::writeTripCandidate);
        writeEndTag("TourCandidate");
    }

    private void writeTripCandidate(TripCandidate tripCandidate) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("mode", tripCandidate.getMode()));
        attributes.add(Tuple.of("utility", ""+tripCandidate.getUtility()));
        writeStartTag("TripCandidate", attributes);
        if(tripCandidate instanceof DefaultRoutedTripCandidate) {
            DefaultRoutedTripCandidate routedTripCandidate = (DefaultRoutedTripCandidate) tripCandidate;
            for(PlanElement planElement: routedTripCandidate.getRoutedPlanElements()) {
                if(planElement instanceof Activity) {
                    this.writeActivity((Activity) planElement);
                }
                else if (planElement instanceof Leg) {
                    this.writeLeg((Leg) planElement);
                }
            }
        }
        writeEndTag("TripCandidate");
    }

    private void writeActivity(Activity activity) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("type", activity.getType()));
        writeStartTag("activity", attributes);
        writeEndTag("activity");
    }

    private void writeLeg(Leg leg) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("mode", leg.getMode()));
        writeStartTag("leg", attributes);
        writeEndTag("leg");
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        System.out.println("Starting iteration with " + this.tourSelectorEvents.size() + " stored events");
        this.eventsManager.addHandler(this);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        this.write(this.matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "tourSelection.xml"));
        this.tourSelectorEvents.clear();
        this.eventsManager.removeHandler(this);
    }
}
