package com.powsybl.gse.map;

import javafx.geometry.Point2D;

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

    private final TileManager manager;

    public TilePoint(double x, double y, int zoom, TileManager manager) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.manager = Objects.requireNonNull(manager);
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
        return new Tile((int) Math.floor(x), (int) Math.floor(y), zoom, manager);
    }

    public Coordinate getCoordinate() {
        int n = 1 << zoom;

        // project from tile space point to web mercator space
        double x2 = x / n * 360 - 180;
        double y2 = -(2 * y / n - 1) * Math.PI;

        // project from web mercator space to WGS84 (GPS) space
        return WebMercatorProjection.unproject(x2, y2);
    }
}
