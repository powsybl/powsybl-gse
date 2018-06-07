package com.powsybl.gse.map;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SegmentGraphic {

    private final Coordinate coordinate1;

    private final Coordinate coordinate2;

    public SegmentGraphic(Coordinate coordinate1, Coordinate coordinate2) {
        this.coordinate1 = Objects.requireNonNull(coordinate1);
        this.coordinate2 = Objects.requireNonNull(coordinate2);
    }

    public Coordinate getCoordinate1() {
        return coordinate1;
    }

    public Coordinate getCoordinate2() {
        return coordinate2;
    }

    @Override
    public String toString() {
        return "SegmentGraphic(coordinate1=" + coordinate1 + ", coordinate2=" + coordinate2 + ")";
    }
}
