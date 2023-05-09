package org.eqasim.switzerland.drt.config_group;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class DrtZonalSystemParamSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String SET_NAME = "zonalSystem";
    public static final String ZONES_GENERATION = "zonesGeneration";
    public static final String SHAPEFILE = "shapefile";
    public static final String ZONE_SHAPE = "zoneShape";

    public static final String CELL_SIZE = "cellSize";


    //options
    public enum ZonesGeneration {GridFromNetwork, Shapefile}

    private ZonesGeneration zonesGeneration = ZonesGeneration.GridFromNetwork;
    @PositiveOrZero
    private int cellSize = 500;

    public enum ZoneShape {Hexagon, Square}

    private ZoneShape zoneShape = ZoneShape.Hexagon;

    private String shapefile = "";

    public DrtZonalSystemParamSet() {
        super(SET_NAME);
    }

    @StringGetter(ZONES_GENERATION)
    public ZonesGeneration getZonesGeneration() {
        return this.zonesGeneration;
    }

    @StringSetter(ZONES_GENERATION)
    public void setZonesGeneration(ZonesGeneration zonesGeneration) {
        this.zonesGeneration = zonesGeneration;
    }

    @StringGetter(SHAPEFILE)
    public String getShapefile() {
        return this.shapefile;
    }

    @StringSetter(SHAPEFILE)
    public void setShapefile(String shapefile) {
        this.shapefile = shapefile;
    }

    @StringGetter(ZONE_SHAPE)
    public ZoneShape getZoneShape() {
        return this.zoneShape;
    }

    @StringSetter(ZONE_SHAPE)
    public void setZoneShape(ZoneShape zoneShape) {
        this.zoneShape = zoneShape;
    }

    @StringGetter(CELL_SIZE)
    public int getCellSize() {
        return this.cellSize;
    }

    @StringSetter(CELL_SIZE)
    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }
}
