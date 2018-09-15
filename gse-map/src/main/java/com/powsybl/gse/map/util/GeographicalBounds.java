/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.util;

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

    public boolean contains(Coordinate c) {
        Objects.requireNonNull(c);
        double minLon = Math.min(c1.getLon(), c2.getLon());
        double minLat = Math.min(c1.getLat(), c2.getLat());
        double maxLon = Math.max(c1.getLon(), c2.getLon());
        double maxLat = Math.max(c1.getLat(), c2.getLat());
        return c.getLon() >= minLon && c.getLon() <= maxLon
                && c.getLat() > minLat && c.getLat() <= maxLat;
    }

    @Override
    public String toString() {
        return "GeographicalBounds(c1=" + c1 + ", c2=" + c2 + ")";
    }
}
