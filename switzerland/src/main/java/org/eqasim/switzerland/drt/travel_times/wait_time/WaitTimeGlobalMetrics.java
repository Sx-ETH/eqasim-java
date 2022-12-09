package org.eqasim.switzerland.drt.wait_time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class WaitTimeGlobalMetrics {
	// Computes the waitTime average for all the drt trips in the previous iteration or iterations (depending on the mode)
	private static final Map<Integer,Set<DrtTripData>> iterationsDrtTrips = new HashMap<>();
	private static final Map<Integer,Double> iterationsSuccessiveAvg = new HashMap<>();
	private static final Logger logger = Logger.getLogger(WaitTimeGlobalMetrics.class);
	
	public static double calculateAverageWaitTime(Set<DrtTripData> drtTrips) {
		double average = 0.0;
		int observations = 0;
		
		
		for (DrtTripData drtTrip : drtTrips) {
			//logger.info(drtTrip);
			if (!drtTrip.rejected){
				average += drtTrip.waitTime;
				observations += 1;
			}
		}
		average = average / observations;
		
		return average;
	}
	
	public static double calculateMovingAverageWaitTime(Set<DrtTripData> drtTrips, int iteration, int movingWindow) {
		Set<DrtTripData> iterationDrtTrips = new HashSet<>();
		
		// We have to add them to a new set because if not then it would be a reference and the reset after each iteration would delete the trips
		iterationDrtTrips.addAll(drtTrips);
		//update with current drt trips
		iterationsDrtTrips.put(iteration, iterationDrtTrips);
		
		int start = 0;
		if (iteration > 0 && iteration >= movingWindow) {
			start = iteration - movingWindow + 1;
		}
		
		logger.info("Iteration " + String.valueOf(iteration));
		
		Set<DrtTripData> allDrtTrips = new HashSet<>();
		
        for (int i = start; i<=iteration; i++){
        	allDrtTrips.addAll(iterationsDrtTrips.get(i));
			logger.info("Iteration " + String.valueOf(i) + " iterationsDrtTrips size: " + String.valueOf(iterationsDrtTrips.get(i).size()));
        }
        
        

        logger.info("Iteration " + String.valueOf(iteration) + " allDrtTrips size: " + String.valueOf(allDrtTrips.size()));

        //Find total averages for the time period
        return calculateAverageWaitTime(allDrtTrips);
        
        
	}
	
	public static double calculateMethodOfSuccessiveAverageWaitTime(Set<DrtTripData> drtTrips, int iteration, double weight) {
		double iterationAvg = calculateAverageWaitTime(drtTrips);
		if (iteration == 0) {
			iterationsSuccessiveAvg.put(iteration, iterationAvg);
			return iterationAvg;
		}
		double previousAvg = iterationsSuccessiveAvg.get(iteration - 1);
		if (Double.isNaN(previousAvg)) {
			iterationsSuccessiveAvg.put(iteration, iterationAvg);
			return iterationAvg;
		}
		// Think about what to do in case we don't have drtTrips (then iterationAvg is nan)
		double newAvg = (1 - weight) * previousAvg + weight * iterationAvg;
		iterationsSuccessiveAvg.put(iteration, newAvg);
		
		return newAvg;
		
	}
	
}
