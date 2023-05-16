package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

public class NoTourFoundForPersonEvent extends Event {

    public final static String EVENT_TYPE = "NoTourFoundForPersonEvent";

    private final Person person;

    public NoTourFoundForPersonEvent(double time, Person person) {
        super(time);
        this.person = person;
    }

    @Override
    public String getEventType() {
        return null;
    }

    public Person getPerson() {
        return this.person;
    }
}
