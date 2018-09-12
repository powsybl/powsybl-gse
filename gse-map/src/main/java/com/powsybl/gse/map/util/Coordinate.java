/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.util;

import java.util.Objects;

/**
 * GPS coordinate.
 *
 * @see <a href="https://en.wikipedia.org/wiki/World_Geodetic_System"/>https://en.wikipedia.org/wiki/World_Geodetic_System</a>
 *
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
    public int hashCode() {
        return Objects.hash(lon, lat);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coordinate) {
            Coordinate c = (Coordinate) obj;
            return c.lon == lon && c.lat == lat;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Coordinate(" + lon + ", " + lat + ")";
    }
}
