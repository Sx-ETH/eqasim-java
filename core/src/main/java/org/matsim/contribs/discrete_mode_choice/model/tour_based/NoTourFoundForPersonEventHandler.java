package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.matsim.core.events.handler.EventHandler;

public interface NoTourFoundForPersonEventHandler extends EventHandler {

    void handleEvent(NoTourFoundForPersonEvent event);
}
