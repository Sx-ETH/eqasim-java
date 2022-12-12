package org.eqasim.switzerland.drt;

import org.matsim.core.config.CommandLine;

public class SimulationParameter {
    //for now get all the drt time reliability configs to pass through here


    private boolean useAverageWaitTime;
    private boolean useDelayFactor;

    private String delayCalcMethod;

    private String waitTimeCalcMethod;
    private double timeWindowMin;

    private String zonalPath;

    private double MsaWeight;

    private int movingWindow;

    private boolean writeDetailedDelayStats;
    private double simEndTime;

    private double drtOperationTime;

    //toDo: would have to change from commandline to config file

    public void setWaitTimeParams (CommandLine cmd) {
        //method of calculating wait times
    }

    public void setDelayTimeParams (CommandLine cmd) {
        //method of calculating delay times
    }

    public void setDrtTravelTimeParams (CommandLine cmd) {
        //movingWindow
        //Weight
        //simEndTime
        //drtOperationtime


        if (cmd.hasOption("use-avgwaittime")) {
            setUseAverageWaitTime(Boolean.parseBoolean(cmd.getOption("use-avgwaittime").get()));
        } else {
            setUseAverageWaitTime(true);
        }

        if (cmd.hasOption("use-delayfactor")) {
            setUseAverageWaitTime(Boolean.parseBoolean(cmd.getOption("use-delayfactor").get()));
        } else {
            setUseDelayFactor(true);
        }

        if (isUseAverageWaitTime()){
            //set params
        }

        if (isUseDelayFactor()) {
            //set params
        }

        if (cmd.hasOption("time-windowMin")) {
            setTimeWindow(Double.parseDouble(cmd.getOption("time-windowMin").get()));
        } else {
            setTimeWindow(5); //default is 5? set a default based on our findings later
        }

    }

    private void setUseAverageWaitTime(boolean useAverageWaitTime) {
        this.useAverageWaitTime = useAverageWaitTime;
    }

    private boolean isUseAverageWaitTime(){return useAverageWaitTime; }

    private void setUseDelayFactor(boolean useDelayFactor) {
        this.useDelayFactor = useDelayFactor;
    }

    private boolean isUseDelayFactor(){return useDelayFactor; }


    private void setTimeWindow(double timeWindow) {
        if (timeWindow > 0 | timeWindow <= simEndTime*60) {
            this.timeWindowMin = timeWindow;
        } else {
            throw new IllegalStateException("Invalid time window: " + timeWindow + " .The time window should be in minutes within simulation time");
        }
    }
    public double getTimeWindow_Min() { return timeWindowMin; }


}
