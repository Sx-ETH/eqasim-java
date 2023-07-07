package org.eqasim.ile_de_france.drt.analysis;

import org.eqasim.core.components.EqasimMainModeIdentifier;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import java.util.List;

public class DrtMainModeIdentifier implements MainModeIdentifier {
    private MainModeIdentifier delegate = new EqasimMainModeIdentifier();

    @Override
    public String identifyMainMode(List<? extends PlanElement> planElements) {
        for(PlanElement planElement : planElements) {
            if(planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if(activity.getType().equals("drt interaction")) {
                    return "drt";
                }
            }
        }
        return delegate.identifyMainMode(planElements);
    }
}
