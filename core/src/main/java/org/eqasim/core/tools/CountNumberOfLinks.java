package org.eqasim.core.tools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class CountNumberOfLinks {
    public static void main(String[] args) {

        String networkfile = args[0];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);


        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkfile);

        ScenarioUtils.loadScenario(scenario);


        System.out.println("Number of links: " + network.getLinks().size());

    }
}

