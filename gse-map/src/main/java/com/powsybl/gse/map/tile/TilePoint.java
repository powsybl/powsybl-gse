/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.tile;

import com.powsybl.gse.map.util.Coordinate;

import java.util.Objects;

/**
 * A point in the tile view.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TilePoint {

    private final double x;

    private final double y;

    private final int zoom;

    private final TileServerInfo serverInfo;

    public TilePoint(double x, double y, int zoom, TileServerInfo serverInfo) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.serverInfo = Objects.requireNonNull(serverInfo);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getZoom() {
        return zoom;
    }

    public Tile getTile() {
        return new Tile((int) Math.floor(x), (int) Math.floor(y), zoom, serverInfo);
    }

    public Coordinate getCoordinate() {
        int n = 1 << zoom;

        // project from tile space point to web mercator space
        double x2 = x / n * 360 - 180;
        double y2 = -(2 * y / n - 1) * Math.PI;

        // project from web mercator space to WGS84 (GPS) space
        return WebMercatorProjection.unproject(x2, y2);
    }

    @Override
    public String toString() {
        return "TilePoint(x=" + x + ", y=" + y + ", zoom=" + zoom + ")";
    }
}
