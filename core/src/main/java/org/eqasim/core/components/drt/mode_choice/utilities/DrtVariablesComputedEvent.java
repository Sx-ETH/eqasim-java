package org.eqasim.core.components.drt.mode_choice.utilities;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtVariablesComputedEvent extends Event{

    private final DrtVariables drtVariables;
    private final Person person;
    private final DiscreteModeChoiceTrip modeChoiceTrip;
    private final List<? extends PlanElement> planElements;

    public DrtVariablesComputedEvent(double time, Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> planElements, DrtVariables drtVariables) {
        super(time);
        this.person = person;
        this.modeChoiceTrip = trip;
        this.planElements = planElements;
        this.drtVariables = drtVariables;
    }

    @Override
    public String getEventType() {
        return "DrtVariablesComputed";
    }

    public DrtVariables getDrtVariables() {
        return drtVariables;
    }

    public Person getPerson() {
        return person;
    }

    public DiscreteModeChoiceTrip getTrip() {
        return modeChoiceTrip;
    }

    public List<? extends PlanElement> getPlanElements() {
        return planElements;
    }
}
