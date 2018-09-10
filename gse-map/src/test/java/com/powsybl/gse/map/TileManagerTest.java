/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TileManagerTest {

    private TileManager manager;

    @Before
    public void setUp() {
        TileCache cache = Mockito.mock(TileCache.class);
        TileHttpClient httpClient = Mockito.mock(TileHttpClient.class);
        manager = new TileManager(TileServerInfo.OPEN_CYCLE_MAP, httpClient, cache);
    }

    @After
    public void tearDown() {
        manager.close();
    }

    @Test
    public void test() {
        Coordinate c = new Coordinate(2.162, 48.801);
        TilePoint p = manager.project(c, 10);
        assertEquals(518.1496888888889, p.getX(), 0);
        assertEquals(352.5253223982303, p.getY(), 0);
        assertEquals(10, p.getZoom());
        assertEquals(c.getLat(), p.getCoordinate().getLat(), 0.001);
        assertEquals(c.getLon(), p.getCoordinate().getLon(), 0.001);
    }
}
