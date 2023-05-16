package org.eqasim.ile_de_france.drt;

import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;

public class IDFDrtConfigurator extends IDFConfigurator {

    public IDFDrtConfigurator() {
        super();
        this.configGroups.add(new DvrpConfigGroup());
        this.configGroups.add(new MultiModeDrtConfigGroup());
        this.configGroups.add(new IDFDrtConfigGroup());
    }


    @Override
    public void configureScenario(Scenario scenario) {
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
        DvrpConfigGroup dvrpConfigGroup = (DvrpConfigGroup) scenario.getConfig().getModules().get(DvrpConfigGroup.GROUP_NAME);
        
    }
}
