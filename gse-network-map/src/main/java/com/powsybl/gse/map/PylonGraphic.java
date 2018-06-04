package com.powsybl.gse.map;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PylonGraphic implements Comparable<PylonGraphic> {

    private final Coordinate coordinate;

    private final int num;

    public PylonGraphic(Coordinate coordinate, int num) {
        this.coordinate = Objects.requireNonNull(coordinate);
        this.num = num;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public int getNum() {
        return num;
    }

    @Override
    public int compareTo(PylonGraphic o) {
        return Integer.compare(num, o.num);
    }

    @Override
    public String toString() {
        return "PylonGraphic(coordinate=" + coordinate + ", num=" + num + ")";
    }
}
