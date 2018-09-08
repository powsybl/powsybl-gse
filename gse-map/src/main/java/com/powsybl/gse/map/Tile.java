package com.powsybl.gse.map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Maybe;
import org.asynchttpclient.Response;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

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
        return space.getDescriptor().getUrlTemplate().instanciate(this);
    }

    private static Optional<InputStream> getResponseBodyAsStream(Response response) {
        return response.getStatusCode() == HttpResponseStatus.OK.code()
                ? Optional.of(response.getResponseBodyAsStream())
                : Optional.empty();
    }

    private static TileImage getTileImage(Response response) {
        return () -> getResponseBodyAsStream(response);
    }

    public Maybe<TileImage> request() {
        return space.getCache().readImage(this)
                .switchIfEmpty(space.getHttpClient()
                        .request(Tile.this)
                        .map(Tile::getTileImage));
    }
}
