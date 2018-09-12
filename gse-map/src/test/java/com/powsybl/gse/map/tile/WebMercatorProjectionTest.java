/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.tile;

import com.powsybl.gse.map.util.Coordinate;
import javafx.geometry.Point2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WebMercatorProjectionTest {

    @Test
    public void test() {
        Coordinate c = new Coordinate(2.162, 48.801);
        Point2D p = WebMercatorProjection.project(c);
        assertEquals(new Point2D(2.162, 0.9785243663814854), p);
        assertEquals(c, WebMercatorProjection.unproject(p));
    }
}
