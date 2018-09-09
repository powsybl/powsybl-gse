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

    private final TileSpace space;

    public Tile(int x, int y, int zoom, TileSpace space) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.space = Objects.requireNonNull(space);
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
        return space.getServerInfo().getUrlTemplate().instanciate(this);
    }

    public String getServerName() {
        return space.getServerInfo().getServerName();
    }

    public Maybe<InputStream> request() {
        return space.getCache().readTile(this)
                .switchIfEmpty(space.getHttpClient()
                        .request(Tile.this)
                        .filter(response -> response.getStatusCode() == HttpResponseStatus.OK.code())
                        .map(response ->
                            // also write new downloaded tile to cache
                            new TeeInputStream(response.getResponseBodyAsStream(),
                                               space.getCache().writeTile(this))
                        ));
    }
}
