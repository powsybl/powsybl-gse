/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.gse.map.util.Coordinate;

import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GeographicalBounds {

    private final Coordinate c1;

    private final Coordinate c2;

    public GeographicalBounds(Coordinate c1, Coordinate c2) {
        this.c1 = Objects.requireNonNull(c1);
        this.c2 = Objects.requireNonNull(c2);
    }

    public Coordinate getC1() {
        return c1;
    }

    public Coordinate getC2() {
        return c2;
    }

    @Override
    public String toString() {
        return "GeographicalBounds(c1=" + c1 + ", c2=" + c2 + ")";
    }
}
