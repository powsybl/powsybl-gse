/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.tile;

import com.powsybl.gse.map.util.Coordinate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TileManager implements AutoCloseable {

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

    public Tile createTile(int x, int y, int zoom) {
        return new Tile(x, y, zoom, serverInfo.get());
    }

    public TileHttpClient getHttpClient() {
        return httpClient;
    }

    public int getTileCount(int zoom) {
        serverInfo.get().checkZoomLevel(zoom);
        return 1 << zoom;
    }

    /**
     * Project a GPS coordinate to tile space
     *
     * @param c the GPS coordinate
     * @param zoom the zoom level
     * @return the tile point in the tile space
     */
    public TilePoint project(Coordinate c, int zoom) {
        Objects.requireNonNull(c);
        serverInfo.get().checkZoomLevel(zoom);

        // Project the coordinates to the Mercator projection (from EPSG:4326 to EPSG:3857)
        Point2D p = WebMercatorProjection.project(c);

        // Transform range of x and y to 0 – 1 and shift origin to top left corner
        double x = (p.getX() + 180) / 360;
        double y = (1 - (p.getY() / Math.PI)) / 2;

        // Calculate the number of tiles across the map, n, using 2^zoom
        int n = getTileCount(zoom);

        // Multiply x and y by n
        double xTile = x * n;
        double yTile = y * n;

        return new TilePoint(xTile, yTile, zoom, serverInfo.get());
    }

    public void project(List<Coordinate> coordinates, int zoom, double[] x, double[] y) {
        Objects.requireNonNull(coordinates);
        serverInfo.get().checkZoomLevel(zoom);

        // Calculate the number of tiles across the map, n, using 2^zoom
        int n = getTileCount(zoom);

        // Project the coordinates to the Mercator projection (from EPSG:4326 to EPSG:3857)
        WebMercatorProjection.project(coordinates, x, y);

        // Transform range of x and y to 0 – 1 and shift origin to top left corner
        for (int i = 0; i < coordinates.size(); i++) {
            x[i] = (x[i] + 180) / 360 * n;
            y[i] = (1 - (y[i] / Math.PI)) / 2 * n;
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
