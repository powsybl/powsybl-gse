package com.powsybl.gse.map;

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

    public Tile getTile() {
        return new Tile((int) Math.floor(x), (int) Math.floor(y), zoom, space);
    }

    public Coordinate getCoordinate() {
        throw new AssertionError();
    }
}
