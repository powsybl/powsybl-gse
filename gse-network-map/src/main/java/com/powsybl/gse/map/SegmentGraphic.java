/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SegmentGraphic {

    private final Coordinate coordinate1;

    private final Coordinate coordinate2;

    private final LineGraphic line;

    public SegmentGraphic(Coordinate coordinate1, Coordinate coordinate2, LineGraphic line) {
        this.coordinate1 = Objects.requireNonNull(coordinate1);
        this.coordinate2 = Objects.requireNonNull(coordinate2);
        this.line = Objects.requireNonNull(line);
    }

    public Coordinate getCoordinate1() {
        return coordinate1;
    }

    public Coordinate getCoordinate2() {
        return coordinate2;
    }

    public LineGraphic getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "SegmentGraphic(coordinate1=" + coordinate1 + ", coordinate2=" + coordinate2 + ")";
    }
}
