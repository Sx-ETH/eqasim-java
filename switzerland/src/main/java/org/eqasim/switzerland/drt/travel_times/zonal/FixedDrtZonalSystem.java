package org.eqasim.switzerland.drt.travel_times.zonal;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class FixedDrtZonalSystem {
    protected Map<Id<Link>, String> link2zone = new HashMap<>();
    protected Network network;
    protected Map<String, PreparedGeometry> zones = new HashMap<>();
    private static final Logger log = LogManager.getLogger(FixedDrtZonalSystem.class);

    public Geometry getZone(String zone) {
        return (Geometry) zones.get(zone);
    }

    public abstract String getZoneForLinkId(Id<Link> linkId);

    public Map<String, PreparedGeometry> getZones() {
        return this.zones;
    }

    public void writeLink2Zone(String fileName) {
        String delimiter = ";";
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        log.info("Link2zone size: " + this.link2zone.size());
        int nWrites = 0;
        try {
            writer.append("link_id;zone");
            for (Map.Entry<Id<Link>, String> entry : this.link2zone.entrySet()) {
                writer.append("\n");
                writer.append(Id.writeId(entry.getKey()));
                writer.append(delimiter);
                writer.append(entry.getValue());
                nWrites += 1;

            }
            writer.flush();
            writer.close();
            if (nWrites < this.link2zone.size()) {
                log.warn("There are less writes in the link2zone file than in the map, be careful when using it");
                log.warn("nWrites: " + nWrites);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
