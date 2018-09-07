package com.powsybl.gse.map;

import io.reactivex.Maybe;
import org.asynchttpclient.Response;

import java.util.Objects;

/**
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
        return manager.getDescriptor().getUrlTemplate().instanciate(this);
    }

    public Maybe<Response> request() {
        return manager.getHttpClient().request(this);
    }

    public Coordinate getCoordinate() {
        throw new AssertionError();
    }
}
