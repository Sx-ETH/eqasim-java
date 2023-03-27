package org.eqasim.switzerland.drt;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;

import java.util.ArrayList;
import java.util.Arrays;

public class SimulationParameter {
    // for now get all the drt time reliability configs to pass through here

    private final Config config;
    private boolean useAverageWaitTime;
    private boolean useDelayFactor;

    private String delayCalcMethod;

    private String waitTimeMethod;
    private double timeWindowMin;

    private String zonalPath; // todo

    private double MsaWeight;

    private int movingWindow;

    private boolean writeDetailedDelayStats = true; // todo

    private double drtOperationTime = 24 * 3600; // todo add drt operation time

    private final ArrayList<String> METHODS = new ArrayList<>(Arrays.asList("global", "successive_global",
            "moving_global", "moving_zonal", "successive_zonal", "avg_zonal"));

    private String delayFactorFeedback;

    private String waitTimeFeedback;

    public SimulationParameter(Config config) {
        this.config = config;
    }

    // toDo: would have to change from commandline to config file
    public void setWaitTimeParams(CommandLine cmd) {
        // method of calculating wait times
    }

    public void setDelayTimeParams(CommandLine cmd, Config config) {
        // method of calculating delay times
    }

    public void setDrtTravelTimeParams(CommandLine cmd) {
        // simEndTime
        // drtOperationtime
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

        if (isUseAverageWaitTime()) {
            // set params
            if (cmd.hasOption("waittime-method")) {
                setWaitTimeMethod(cmd.getOption("waittime-method").get());
            } else {
                setWaitTimeMethod("global"); // default is 5? toDo set a default based on our findings later or throw
                // error?
            }
        }

        if (isUseDelayFactor()) {
            // set params
            if (cmd.hasOption("delay-method")) {
                setDelayCalcMethod(cmd.getOption("delay-method").get());
            } else {
                setDelayCalcMethod("global"); // default is 5? toDo set a default based on our findings later or throw
                // error?
            }
        }

        if (cmd.hasOption("time-windowMin")) {
            setTimeWindow(Double.parseDouble(cmd.getOption("time-windowMin").get()));
        } else {
            setTimeWindow(60); // default is 5? toDo set a default based on our findings later or throw error?
        }

        if (cmd.hasOption("moving-window")) {
            setMovingWindow(Integer.parseInt(cmd.getOption("moving-window").get()));
        } else {
            setMovingWindow(5); // default is 5? toDo set a default based on our findings later or throw error?
        }

        if (cmd.hasOption("msa-weight")) {
            setMsaWeight(Double.parseDouble(cmd.getOption("msa-weight").get()));
        } else {
            setMsaWeight(0.5); // default? toDo set a default based on our findings later or throw error?
        }

        if (cmd.hasOption("delayFactorFeedback")) {
            setDelayFactorFeedback(cmd.getOption("delayFactorFeedback").get());
        } else {
            setDelayFactorFeedback("average");
        }

        if (cmd.hasOption("waitTimeFeedback")) {
            setWaitTimeFeedback(cmd.getOption("waitTimeFeedback").get());
        } else {
            setWaitTimeFeedback("average");
        }
    }

    private void setWaitTimeMethod(String waitTimeMethod) {
        if (METHODS.contains(waitTimeMethod)) {
            // define to use zone or not
            if (waitTimeMethod.contains("global")) {
                switch (waitTimeMethod) {
                    case "successive_global":
                        this.waitTimeMethod = "successive";
                        break;
                    case "moving_global":
                        this.waitTimeMethod = "moving";
                        break;
                    default:
                        this.waitTimeMethod = waitTimeMethod;

                }

            } else {
                this.waitTimeMethod = waitTimeMethod; // returns zonal

            }

        } else {
            throw new IllegalStateException("Invalid value. Expecting one of these: " + METHODS + " .");

        }
        // define what the options should be for methods above
    }

    public String getWaitTimeMethod() {
        return this.waitTimeMethod;

    }

    private void setDelayCalcMethod(String delayMethod) {
        if (METHODS.contains(delayMethod)) {
            // define to use zone or not
            if (delayMethod.contains("global")) {
                switch (delayMethod) {
                    case "successive_global":
                        this.delayCalcMethod = "successive";
                        break;
                    case "moving_global":
                        this.delayCalcMethod = "moving";
                        break;
                    default:
                        this.delayCalcMethod = delayMethod;

                }
            } else {
                // it is zonal
                this.delayCalcMethod = delayMethod;

            }

        } else {
            throw new IllegalStateException("Invalid value. Expecting one of these: " + METHODS + ".");

        }
        // define what the options should be for methods above

    }

    public String getDelayCalcMethod() {
        return this.delayCalcMethod;
    }

    private void setMovingWindow(int movingWindow) {
        if (movingWindow > 0 | movingWindow <= config.controler().getLastIteration()) {
            this.movingWindow = movingWindow;
        } else {
            throw new IllegalStateException(
                    "Invalid weight value: " + movingWindow + " . The weight value should be between 0 and 1");
        }
    }

    public int getMovingWindow() {
        return this.movingWindow;
    }

    private void setMsaWeight(double weight) {
        if (weight > 0 | weight < 1) {
            this.MsaWeight = weight;
        } else {
            throw new IllegalStateException(
                    "Invalid weight value: " + weight + " . The weight value should be between 0 and 1");
        }

    }

    public double getMsaWeight() {
        return MsaWeight;
    }

    private void setUseAverageWaitTime(boolean useAverageWaitTime) {
        this.useAverageWaitTime = useAverageWaitTime;
    }

    public boolean isUseAverageWaitTime() {
        return useAverageWaitTime;
    }

    private void setUseDelayFactor(boolean useDelayFactor) {
        this.useDelayFactor = useDelayFactor;
    }

    public boolean isUseDelayFactor() {
        return useDelayFactor;
    }

    private void setTimeWindow(double timeWindow) {
        if (timeWindow > 0 | timeWindow <= config.qsim().getEndTime().seconds() / 60) {
            this.timeWindowMin = timeWindow;
        } else {
            throw new IllegalStateException("Invalid time window: " + timeWindow
                    + " .The time window should be in minutes within simulation time");
        }
    }

    public double getTimeWindow_Min() {
        return timeWindowMin;
    }

    public String getDelayFactorFeedback() {
        return delayFactorFeedback;
    }

    public void setDelayFactorFeedback(String delayFactorFeedback) {
        this.delayFactorFeedback = delayFactorFeedback;
    }

    public String getWaitTimeFeedback() {
        return waitTimeFeedback;
    }

    public void setWaitTimeFeedback(String waitTimeFeedback) {
        this.waitTimeFeedback = waitTimeFeedback;
    }

}
