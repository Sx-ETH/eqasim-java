package org.eqasim.ile_de_france.drt.analysis;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;

public class DvrpAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		bind(DvrpAnalysisListener.class).in(Singleton.class);
		bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class);
		addControlerListenerBinding().to(DvrpAnalysisListener.class);
	}
}
