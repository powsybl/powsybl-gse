package com.powsybl.gse.map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Coordinate {

    private final double lon;
    private final double lat;

    public Coordinate(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    @Override
    public String toString() {
        return "Coordinate(" + lon + ", " + lat + ")";
    }
}
