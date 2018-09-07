package com.powsybl.gse.map;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileManager implements AutoCloseable {

    static final TileDescriptor DEFAULT_TILE_DESCRIPTOR = new TileDescriptor(new TileUrlTemplate("http://tile.openstreetmap.org/${z}/${x}/${y}.png"), 256, 256, 0, 19);

    private final TileDescriptor descriptor;

    private final TileHttpClient httpClient;

    public TileManager() {
        this(DEFAULT_TILE_DESCRIPTOR);
    }

    public TileManager(TileDescriptor descriptor) {
        this(descriptor, new LocalFileSystemTileCache());
    }

    public TileManager(TileDescriptor descriptor, TileCache cache) {
        this.descriptor = Objects.requireNonNull(descriptor);
        httpClient = new TileHttpClient(cache);
    }

    public TileDescriptor getDescriptor() {
        return descriptor;
    }

    TileHttpClient getHttpClient() {
        return httpClient;
    }

    public Tile getTile(Coordinate c, int zoom) {
        int n = 1 << zoom;
        int xTile = (int) Math.floor((c.getLon() + 180) / 360 * n);
        int yTile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(c.getLat())) + 1 / Math.cos(Math.toRadians(c.getLat()))) / Math.PI) / 2 * n);

        // fix tile bounds
        if (xTile < 0) {
            xTile = 0;
        }
        if (xTile >= n) {
            xTile = n - 1;
        }
        if (yTile < 0) {
            yTile = 0;
        }
        if (yTile >= n) {
            yTile = n - 1;
        }

        return new Tile(xTile, yTile, zoom, this);
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
