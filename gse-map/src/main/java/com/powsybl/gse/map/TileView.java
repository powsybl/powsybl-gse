package com.powsybl.gse.map;

import javafx.geometry.Point2D;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileView implements AutoCloseable {

    static final TileDescriptor DEFAULT_TILE_DESCRIPTOR = new TileDescriptor(new TileUrlTemplate("http://tile.openstreetmap.org/${z}/${x}/${y}.png"), 256, 256, 0, 19);

    private final TileDescriptor descriptor;

    private final TileHttpClient httpClient;

    public TileView() {
        this(DEFAULT_TILE_DESCRIPTOR);
    }

    public TileView(TileDescriptor descriptor) {
        this(descriptor, new LocalFileSystemTileCache());
    }

    public TileView(TileDescriptor descriptor, TileCache cache) {
        this.descriptor = Objects.requireNonNull(descriptor);
        httpClient = new TileHttpClient(cache);
    }

    public TileDescriptor getDescriptor() {
        return descriptor;
    }

    TileHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Project a GPS coordinate to tile view
     *
     * @param c the GPS coordinate
     * @param zoom the zoom level
     * @return the tile position in the tile view
     */
    public TilePoint project(Coordinate c, int zoom) {
        // Project the coordinates to the Mercator projection (from EPSG:4326 to EPSG:3857)
        Point2D p = WebMercatorProjection.project(c);

        // Transform range of x and y to 0 – 1 and shift origin to top left corner
        double x = (p.getX() + 180) / 360;
        double y = (1 - (p.getY() / Math.PI)) / 2;

        // Calculate the number of tiles across the map, n, using 2^zoom
        int n = 1 << zoom;

        // Multiply x and y by n
        double xTile = x * n;
        double yTile = y * n;

        return new TilePoint(xTile, yTile, zoom, this);
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
