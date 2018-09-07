/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.geometry.Point2D;

/**
 * Utility class to project GPS coordinate to Web Mercator.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Web_Mercator"/>https://en.wikipedia.org/wiki/Web_Mercator</a>
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class WebMercatorProjection {

    private WebMercatorProjection() {
    }

    public static Point2D project(Coordinate c) {
        double x = c.getLon();
        double y = Math.log(Math.tan(Math.toRadians(c.getLat())) + 1 / Math.cos(Math.toRadians(c.getLat())));
        return new Point2D(x, y);
    }
}
