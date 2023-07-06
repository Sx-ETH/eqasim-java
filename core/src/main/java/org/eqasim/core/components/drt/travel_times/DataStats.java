package org.eqasim.core.components.drt.travel_times;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eqasim.core.components.drt.config_group.DrtDynamicSystemParamSet;
import org.eqasim.core.components.drt.config_group.DrtModeChoiceConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;


public class DataStats {
    private double avg = Double.NaN;
    private double median = Double.NaN;
    private double min = Double.NaN;
    private double p_5 = Double.NaN;
    private double p_25 = Double.NaN;
    private double p_75 = Double.NaN;
    private double p_95 = Double.NaN;
    private double max = Double.NaN;
    private double std = Double.NaN;
    private int nTrips = 0;

    private double weightedAvg = Double.NaN;
    private double weightedStd = Double.NaN;

    private DrtDynamicSystemParamSet.DecayType decayType = null;

    private Id<Request>[] requestIds = null;
    private double[] stats = null;
    private double[] distances = null;
    private boolean useWeightedAvg = false;


    public DataStats() {
    }

    public DataStats(double avg) {
        this.avg = avg;
    }

    public DataStats(double[] stats, Id<Request>[] requestIds) {
        DescriptiveStatistics descStats = new DescriptiveStatistics(stats);
        this.avg = descStats.getMean();
        this.median = descStats.getPercentile(50);
        this.min = descStats.getMin();
        this.p_5 = descStats.getPercentile(5);
        this.p_25 = descStats.getPercentile(25);
        this.p_75 = descStats.getPercentile(75);
        this.p_95 = descStats.getPercentile(95);
        this.max = descStats.getMax();
        this.std = descStats.getStandardDeviation();
        this.nTrips = stats.length;
        this.weightedAvg = this.avg; //To feedback weighted average for delay factor that does not have dynamic implementation
        this.weightedStd = this.std;
        this.requestIds = requestIds;
        this.useWeightedAvg = false;
        this.stats = stats;
    }

    public DataStats(double[] stats, double[] distances, DrtDynamicSystemParamSet.DecayType decayType, Id<Request>[] requestIds) {
        DescriptiveStatistics descStats = new DescriptiveStatistics(stats);
        this.avg = descStats.getMean();
        this.median = descStats.getPercentile(50);
        this.min = descStats.getMin();
        this.p_5 = descStats.getPercentile(5);
        this.p_25 = descStats.getPercentile(25);
        this.p_75 = descStats.getPercentile(75);
        this.p_95 = descStats.getPercentile(95);
        this.max = descStats.getMax();
        this.std = descStats.getStandardDeviation();
        this.nTrips = stats.length;

        this.decayType = decayType;
        calculateWeightedStats(stats, distances);
        this.requestIds = requestIds;
        this.useWeightedAvg = true;
        this.stats = stats;
        this.distances = distances;
    }

    public static DataStats removeTripUsingRequestId(DataStats original, Id<Request> requestId) {
        int i = ArrayUtils.indexOf(original.requestIds, requestId);
        double newStats[] = ArrayUtils.remove(original.stats, i);
        Id<Request> newRequestIds[] = ArrayUtils.remove(original.requestIds, i);
        if (original.useWeightedAvg) {
            double newDistances[] = ArrayUtils.remove(original.distances, i);
            return new DataStats(newStats, newDistances, original.decayType, newRequestIds);
        } else {
            return new DataStats(newStats, newRequestIds);
        }
    }

    private void calculateWeightedStats(double[] stats, double[] distances) {
        double avgSum = 0.0;
        double weightSum = 0.0;
        double[] weights = new double[stats.length];
        double weightsSquaredSum = 0.0;
        double weightedSquaredValuesSum = 0.0;
        for (int i = 0; i < stats.length; i++) {
            // The distances are in meters, so we need to convert them to kilometers
            double d = distances[i] / 1000.0;
            switch (decayType) {
                case POWER_DECAY:
                    weights[i] = Math.pow(d, -2);
                    break;
                case INVERSE_DECAY:
                    weights[i] = Math.pow(d, -1);
                    break;
                case EXPONENTIAL_DECAY:
                    weights[i] = Math.exp(-d);
                    break;
                case SPATIAL_CORRELATION:
                    //ToDo
                    break;
            }
            avgSum += stats[i] * weights[i];
            weightSum += weights[i];
            weightsSquaredSum += Math.pow(weights[i], 2);
            weightedSquaredValuesSum += Math.pow(stats[i], 2) * weights[i];
        }
        this.weightedAvg = avgSum / weightSum;

        double a = (weightedSquaredValuesSum / weightSum) - Math.pow(this.weightedAvg, 2);
        double b = Math.pow(weightSum, 2) / (Math.pow(weightSum, 2) - weightsSquaredSum);
        this.weightedStd = Math.sqrt(a * b); //Computed using Case I from http://seismo.berkeley.edu/~kirchner/Toolkits/Toolkit_12.pdf to have unbiased estimate

        //powerDecay = Sum (value * distance^-2);
        //inverseDecay = Sum (value * 1/distance);
        //exponentialDecay = Sum (value * e^ - distance);
        //spatialCorrelation = *some formula;

    }


    public double getStat(DrtModeChoiceConfigGroup.Feedback stat) {
        switch (stat) {
            case average:
                return avg;
            case median:
                return median;
            case min:
                return min;
            case p_5:
                return p_5;
            case p_25:
                return p_25;
            case p_75:
                return p_75;
            case p_95:
                return p_95;
            case max:
                return max;
            case weightedAverage:
                return weightedAvg;
            default:
                return Double.NaN;
        }
    }

    public double getStd() {
        return std;
    }

    public double getNTrips() {
        return nTrips;
    }

    public double getWeightedStd() {
        return weightedStd;
    }

    public static String getCSVHeader(String separator) {
        return "avg" + separator + "median" + separator + "min" + separator + "p_5" + separator + "p_25" + separator + "p_75" + separator + "p_95" + separator + "max" + separator + "weightedAvg";
    }

    public String getCSVLine(String separator) {
        return avg + separator + median + separator + min + separator + p_5 + separator + p_25 + separator + p_75 + separator + p_95 + separator + max + separator + weightedAvg;
    }

    private static double combineIterations(double previousIt, double currentIt, double weight) {
        // In the case the previous avg is Nan then we only use the current average
        if (Double.isNaN(previousIt)) {
            return currentIt;
        }
        // In the case there are no trips in this iteration then we keep the value
        // estimated in the previous iteration (so we'll never have nans after one
        // correct value)
        if (Double.isNaN(currentIt)) {
            return previousIt;
        }
        return (1 - weight) * previousIt + weight * currentIt;
    }

    public static DataStats combineIterationsSuccessive(DataStats previousIt, DataStats currentIt,
                                                        double weight) {
        DataStats combinedData = new DataStats();

        combinedData.avg = combineIterations(previousIt.avg, currentIt.avg, weight);
        combinedData.median = combineIterations(previousIt.median, currentIt.median, weight);
        combinedData.min = combineIterations(previousIt.min, currentIt.min, weight);
        combinedData.p_5 = combineIterations(previousIt.p_5, currentIt.p_5, weight);
        combinedData.p_25 = combineIterations(previousIt.p_25, currentIt.p_25, weight);
        combinedData.p_75 = combineIterations(previousIt.p_75, currentIt.p_75, weight);
        combinedData.p_95 = combineIterations(previousIt.p_95, currentIt.p_95, weight);
        combinedData.max = combineIterations(previousIt.max, currentIt.max, weight);

        return combinedData;
    }

}
