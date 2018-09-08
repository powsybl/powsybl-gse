package com.powsybl.gse.map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Maybe;
import org.asynchttpclient.Response;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * A point in the tile view.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TilePoint {

    private final double x;

    private final double y;

    private final int zoom;

    private final TileSpace space;

    public TilePoint(double x, double y, int zoom, TileSpace space) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.space = Objects.requireNonNull(space);
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
        return space.getCache().getImage(this)
                              .switchIfEmpty(space.getHttpClient()
                                      .request(TilePoint.this)
                                      .map(TilePoint::getTileImage));
    }

    public Coordinate getCoordinate() {
        throw new AssertionError();
    }
}
