package org.eqasim.core.components.drt.mode_choice.utilities;

import org.matsim.core.events.handler.EventHandler;

public interface DrtVariablesComputedEventHandlerInterface extends EventHandler {
    void handleEvent(DrtVariablesComputedEvent event);
}
