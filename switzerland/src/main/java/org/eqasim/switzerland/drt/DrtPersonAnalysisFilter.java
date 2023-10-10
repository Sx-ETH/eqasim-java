package org.eqasim.switzerland.drt;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DrtPersonAnalysisFilter implements PersonAnalysisFilter {
    @Override
    public boolean analyzePerson(Id<Person> personId) {
        if (personId.toString().contains("drt") || personId.toString().contains("pt")) {
            return false;
        }
        return true;
    }
}
