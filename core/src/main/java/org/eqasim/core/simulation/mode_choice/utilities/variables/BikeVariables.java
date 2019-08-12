package org.eqasim.core.simulation.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.BaseVariables;

public class BikeVariables implements BaseVariables {
	final public double travelTime_min;

	public BikeVariables(double travelTime_min) {
		this.travelTime_min = travelTime_min;
	}
}