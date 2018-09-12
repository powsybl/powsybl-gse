/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class Tile {

    private final int x;

    private final int y;

    private final int zoom;

    private final TileServerInfo serverInfo;

    public Tile(int x, int y, int zoom, TileServerInfo serverInfo) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.serverInfo = Objects.requireNonNull(serverInfo);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZoom() {
        return zoom;
    }

    public String getUrl() {
        return serverInfo.getUrlTemplate().instanciate(this);
    }

    public String getServerName() {
        return serverInfo.getServerName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, zoom, serverInfo.getServerName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tile) {
            Tile other = (Tile) obj;
            return x == other.x && y == other.y && zoom == other.zoom
                    && serverInfo.getServerName().equals(other.serverInfo.getServerName());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Tile(x=" + x + ", y=" + y + ", zoom=" + zoom + ", serverName=" + serverInfo.getServerName() + ")";
    }
}
