/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Maybe;
import org.apache.commons.io.input.TeeInputStream;

import java.io.InputStream;
import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class Tile {

    private final int x;

    private final int y;

    private final int zoom;

    private final TileManager manager;

    public Tile(int x, int y, int zoom, TileManager manager) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.manager = Objects.requireNonNull(manager);
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
        return manager.getServerInfo().getUrlTemplate().instanciate(this);
    }

    public String getServerName() {
        return manager.getServerInfo().getServerName();
    }

    public Maybe<InputStream> request() {
        return manager.getCache().readTile(this)
                .switchIfEmpty(manager.getHttpClient()
                        .request(Tile.this)
                        .filter(response -> response.getStatusCode() == HttpResponseStatus.OK.code())
                        .map(response ->
                            // also write new downloaded tile to cache
                            new TeeInputStream(response.getResponseBodyAsStream(),
                                               manager.getCache().writeTile(this))
                        ));
    }
}
