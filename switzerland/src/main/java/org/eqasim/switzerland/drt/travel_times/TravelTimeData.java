package org.eqasim.switzerland.drt.travel_times;

public class TravelTimeData {
    //wait time data
    public StatData waitTimeStat = new StatData();
    public StatData delayFactorStat = new StatData();

/*    public TravelTimeData(){
        this.waitTimeStat = new StatData();
        this.delayFactorStat = new StatData();
    }*/

    //delay time data
    class StatData {
        public int tripNum;
        public double avg;
        public double median;
        public double min;
        public double p_5;
        public double p_25;
        public double p_75;
        public double p_95;
        public double max;
    }
}


