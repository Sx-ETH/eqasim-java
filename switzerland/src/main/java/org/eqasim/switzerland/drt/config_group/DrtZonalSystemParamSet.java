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
    private String zonesGeneration = "GridFromNetwork";
    @PositiveOrZero
    private int cellSize = 500;

    private String zoneShape = "Hexagon";

    private String shapefile = "";

    public DrtZonalSystemParamSet() {
        super(SET_NAME);
    }

    @StringGetter(ZONES_GENERATION)
    public String getZonesGeneration() {
        return this.zonesGeneration;
    }

    @StringSetter(ZONES_GENERATION)
    public void setZonesGeneration(String zonesGeneration) {
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
    public String getZoneShape() {
        return this.zoneShape;
    }

    @StringSetter(ZONE_SHAPE)
    public void setZoneShape(String zoneShape) {
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
