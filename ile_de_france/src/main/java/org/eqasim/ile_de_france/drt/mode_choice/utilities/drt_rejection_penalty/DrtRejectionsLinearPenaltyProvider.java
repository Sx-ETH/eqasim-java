package org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty;

import com.google.inject.Inject;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrtRejectionsLinearPenaltyProvider implements DrtRejectionPenaltyProvider, IterationEndsListener {

    private final double targetRejectionProbability;
    private final double initialRejectionPenalty;
    private final double rejectionPenaltyAlpha;
    private final static int ENABLE_AFTER_ITERATION = -1;
    private double rejectionPenalty;
    private final RejectionTracker rejectionTracker;
    private final MatsimServices services;
    private final Map<Integer, Double> penaltiesHistory = new HashMap<>();
    private final Map<Integer, Double> rejectionProbabilitiesHistory = new HashMap<>();
    private final Map<Integer, Double> numberOfRequestsToRejectionRates = new HashMap<>();

    private final boolean enableBackwardAdjustment;

    @Inject
    public DrtRejectionsLinearPenaltyProvider(RejectionTracker rejectionTracker, MatsimServices services, DrtRejectionsLinearPenaltyProviderConfigGroup configGroup) {
        this.rejectionTracker = rejectionTracker;
        this.services = services;
        this.targetRejectionProbability = configGroup.getTargetRejectionProbability();
        this.initialRejectionPenalty = configGroup.getInitialRejectionPenalty();
        this.rejectionPenaltyAlpha = configGroup.getAlpha();
        this.rejectionPenalty = this.initialRejectionPenalty;
        this.enableBackwardAdjustment = configGroup.isBackwardAdjustmentEnabled();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int lastNumberOfRequests = this.rejectionTracker.getNumberOfRequests();
        double lastRejectionProbability = this.rejectionTracker.getRejectionProbability();
        this.rejectionProbabilitiesHistory.put(event.getIteration(), lastRejectionProbability);
        if(!this.penaltiesHistory.containsKey(event.getIteration())) {
            this.penaltiesHistory.put(event.getIteration(), initialRejectionPenalty);
        }

        if(!this.numberOfRequestsToRejectionRates.containsKey(lastNumberOfRequests) || this.numberOfRequestsToRejectionRates.get(lastNumberOfRequests) > lastRejectionProbability) {
            this.numberOfRequestsToRejectionRates.put(lastNumberOfRequests, lastRejectionProbability);
        }


        if(!Double.isNaN(lastRejectionProbability) && event.getIteration() >= ENABLE_AFTER_ITERATION){
            if(enableBackwardAdjustment || targetRejectionProbability < lastRejectionProbability) {
                double delta = targetRejectionProbability - lastRejectionProbability;
                double update = delta * rejectionPenaltyAlpha;
                if(this.rejectionPenalty + update <= 0) {
                    this.rejectionPenalty += update;
                }
            }
        }

        if(!event.isLastIteration()){
            this.penaltiesHistory.put(event.getIteration()+1, this.rejectionPenalty);
        }


        try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.services.getControlerIO().getOutputFilename("drt_rejection_penalties.csv"))));
            writer.write("iteration;penalty;lastRejectionRate\n");
            for(Map.Entry<Integer, Double> entry: this.penaltiesHistory.entrySet()){
                writer.write(entry.getKey()+";"+entry.getValue()+";"+this.rejectionProbabilitiesHistory.get(entry.getKey())+"\n");
            }
            writer.close();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.services.getControlerIO().getOutputFilename("requests_to_rejections.csv"))));
            writer.write("numberOfRequests;rejectionRate\n");
            List<Integer> keys = new ArrayList<>(this.numberOfRequestsToRejectionRates.keySet());
            for(Object key: keys.stream().sorted().toArray()) {
                Integer k = (Integer) key;
                writer.write(key+";"+this.numberOfRequestsToRejectionRates.get(k)+"\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public double getRejectionPenalty() {
        return this.rejectionPenalty;
    }
}
