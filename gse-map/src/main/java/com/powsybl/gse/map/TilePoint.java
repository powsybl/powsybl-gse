package com.powsybl.gse.map;

import io.reactivex.Maybe;
import org.asynchttpclient.Response;

import java.util.Objects;

/**
 * A point in the tile view.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TilePoint {

    private final double x;

    private final double y;

    private final int zoom;

    private final TileView view;

    public TilePoint(double x, double y, int zoom, TileView view) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.view = Objects.requireNonNull(view);
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
        return view.getDescriptor().getUrlTemplate().instanciate(this);
    }

    public Maybe<Response> request() {
        return view.getHttpClient().request(this);
    }

    public Coordinate getCoordinate() {
        throw new AssertionError();
    }
}
