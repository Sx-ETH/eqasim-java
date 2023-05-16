package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;


import java.io.FileWriter;
import java.io.IOException;

public class UtilitiesRecorder implements TourSelectorEventHandler, NoTourFoundForPersonEventHandler, BeforeMobsimListener, IterationStartsListener {

    private final MatsimServices matsimServices;

    private final EventsManager eventsManager;

    private final Population population;

    private final boolean lastIterationOnly;


    IdMap<Person, Double> lastUtilities = new IdMap<>(Person.class);

    @Override
    public void handleEvent(TourSelectorEvent tourSelectorEvent) {
        if(!this.lastUtilities.containsKey(tourSelectorEvent.getPerson().getId())) {
            this.lastUtilities.put(tourSelectorEvent.getPerson().getId(), tourSelectorEvent.getSelected().getUtility());
        }
        else{
            double newUtility = this.lastUtilities.get(tourSelectorEvent.getPerson().getId()) + tourSelectorEvent.getSelected().getUtility();
            this.lastUtilities.put(tourSelectorEvent.getPerson().getId(), newUtility);
        }
        this.lastUtilities.put(tourSelectorEvent.getPerson().getId(), tourSelectorEvent.getSelected().getUtility());
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        if(event.isLastIteration() || !this.lastIterationOnly) {
            for(Id<Person> personId: this.population.getPersons().keySet()) {
                if(!this.lastUtilities.containsKey(personId)) {
                    this.lastUtilities.put(personId, Double.NaN);
                }
            }
            String filepath = this.matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "utilities.csv");
            try {
                FileWriter fileWriter = new FileWriter(filepath);
                fileWriter.write("personId;utility\n");
                for(Id<Person> personId: lastUtilities.keySet()) {
                    fileWriter.write(String.format("%s;%f\n", personId.toString(), lastUtilities.get(personId)));
                }
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.lastUtilities.clear();
            this.eventsManager.removeHandler(this);
        }

    }

    public UtilitiesRecorder(MatsimServices matsimServices, EventsManager eventsManager, Population population, boolean lastIterationOnly) {
        this.matsimServices = matsimServices;
        this.eventsManager = eventsManager;
        this.population = population;
        this.lastIterationOnly = lastIterationOnly;

    }

    @Override
    public void handleEvent(NoTourFoundForPersonEvent event) {
        this.lastUtilities.put(event.getPerson().getId(), Double.NaN);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if(event.isLastIteration() || !this.lastIterationOnly) {
            this.eventsManager.addHandler(this);
        }
    }
}
