package org.eqasim.core.components.drt.travel_times;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DrtPredictions {
    private class DrtTripPrediction {
        public double travelTime_min;
        public double accessEgressTime_min;
        public double cost_MU;
        public double waitingTime_min;
        public double euclideanDistance_km;
        public double maxTravelTime_min;
        public double directRideTime_min;
        public Id<Link> startLinkId;
        public double startTime;
        public Person person;
        public DiscreteModeChoiceTrip trip;
    }

    // List containing the predicted variables for each trip
    private final List<DrtTripPrediction> tripPredictions;

    @Inject
    public DrtPredictions() {
        this.tripPredictions = new ArrayList<DrtTripPrediction>();
    }

    public void addTripPrediction(double travelTime_min, double accessEgressTime_min, double cost_MU, double waitingTime_min,
                                  double euclideanDistance_km, double maxTravelTime_min, double directRideTime_min, Id<Link> startLinkId,
                                  double startTime, Person person, DiscreteModeChoiceTrip trip) {
        DrtTripPrediction prediction = new DrtTripPrediction();
        prediction.travelTime_min = travelTime_min;
        prediction.accessEgressTime_min = accessEgressTime_min;
        prediction.cost_MU = cost_MU;
        prediction.waitingTime_min = waitingTime_min;
        prediction.euclideanDistance_km = euclideanDistance_km;
        prediction.maxTravelTime_min = maxTravelTime_min;
        prediction.directRideTime_min = directRideTime_min;
        prediction.startLinkId = startLinkId;
        prediction.startTime = startTime;
        prediction.person = person;
        prediction.trip = trip;
        this.tripPredictions.add(prediction);
    }

    public void clearTripPredictions() {
        this.tripPredictions.clear();
    }

    public void writeTripsPredictions(int iterationNumber, Config config) {
        String filename = "drt_drtTripsPredictions.csv";
        String outputDir = config.controler().getOutputDirectory() + "/ITERS/it." + iterationNumber + "/"
                + iterationNumber + "." + filename;
        BufferedWriter writer = IOUtils.getBufferedWriter(outputDir);
        try {
            String header = "personId;tripIndex;startLink;startTime;travelTime_min;accessEgressTime_min;cost_MU;waitingTime_min;euclideanDistance_km;maxTravelTime_min;directRideTime_min\n";
            writer.write(header);
            for (DrtTripPrediction prediction : this.tripPredictions) {
                writer.append(prediction.person.getId().toString());
                writer.append(";");
                writer.append(Integer.toString(prediction.trip.getIndex()));
                writer.append(";");
                if (prediction.startLinkId == null) {
                    writer.append("null");
                } else {
                    writer.append(prediction.startLinkId.toString());
                }
                writer.append(";");
                writer.append(Double.toString(prediction.startTime));
                writer.append(";");
                writer.append(Double.toString(prediction.travelTime_min));
                writer.append(";");
                writer.append(Double.toString(prediction.accessEgressTime_min));
                writer.append(";");
                writer.append(Double.toString(prediction.cost_MU));
                writer.append(";");
                writer.append(Double.toString(prediction.waitingTime_min));
                writer.append(";");
                writer.append(Double.toString(prediction.euclideanDistance_km));
                writer.append(";");
                writer.append(Double.toString(prediction.maxTravelTime_min));
                writer.append(";");
                writer.append(Double.toString(prediction.directRideTime_min));
                writer.append("\n");
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
