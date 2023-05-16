package org.eqasim.ile_de_france.feeder.analysis;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;

public class FeederAnalysisModule extends AbstractModule {

    @Override
    public void install() {
        bind(FeederAnalysisListener.class).in(Singleton.class);
        bind(MainModeIdentifier.class).to(FeederMainModeIdentifier.class);
        addControlerListenerBinding().to(FeederAnalysisListener.class);
    }
}
