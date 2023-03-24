package org.eqasim.switzerland.drt.travel_times.zonal;


import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface FixedDrtZonalSystem {
	
	public Geometry getZone(String zone);
	
	public String getZoneForLinkId(Id<Link> linkId);
	
	public Map<String, PreparedGeometry> getZones();
	
	public void writeLink2Zone(String fileName);

}
