package com.powsybl.gse.map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileManager implements AutoCloseable {

    private final ObjectProperty<TileServerInfo> serverInfo;

    private final TileHttpClient httpClient;

    private final TileCache cache;

    public TileManager() {
        this(TileServerInfo.OSM_STANDARD);
    }

    public TileManager(TileServerInfo serverInfo) {
        this(serverInfo, new LocalFileSystemTileCache());
    }

    public TileManager(TileServerInfo serverInfo, TileCache cache) {
        this(serverInfo, new TileHttpClient(), cache);
    }

    public TileManager(TileServerInfo serverInfo, TileHttpClient httpClient, TileCache cache) {
        this.serverInfo = new SimpleObjectProperty<>(Objects.requireNonNull(serverInfo));
        this.httpClient = Objects.requireNonNull(httpClient);
        this.cache = Objects.requireNonNull(cache);
    }

    public ObjectProperty<TileServerInfo> serverInfoProperty() {
        return serverInfo;
    }

    public TileServerInfo getServerInfo() {
        return serverInfo.get();
    }

    public TileCache getCache() {
        return cache;
    }

    TileHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Project a GPS coordinate to tile space
     *
     * @param c the GPS coordinate
     * @param zoom the zoom level
     * @return the tile point in the tile space
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
