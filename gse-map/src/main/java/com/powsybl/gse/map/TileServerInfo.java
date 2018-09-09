package com.powsybl.gse.map;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TileServerInfo {

    public static final TileServerInfo OSM_STANDARD = new TileServerInfo("OpenStreetMapStandard", new TileUrlTemplate("http://tile.openstreetmap.org/${z}/${x}/${y}.png"), 256, 256, 0, 19);
    public static final TileServerInfo OPEN_CYCLE_MAP = new TileServerInfo("OpenCycleMap", new TileUrlTemplate("http://tile.thunderforest.com/cycle/${z}/${x}/${y}.png"), 256, 256, 0, 19);

    private final String serverName;

    private final TileUrlTemplate urlTemplate;

    private final double tileWidth;

    private final double tileHeight;

    private final int minZoomLevel;

    private final int maxZoomLevel;

    public TileServerInfo(String serverName, TileUrlTemplate urlTemplate, double tileWidth, double tileHeight, int minZoomLevel, int maxZoomLevel) {
        this.serverName = Objects.requireNonNull(serverName);
        this.urlTemplate = Objects.requireNonNull(urlTemplate);
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
    }

    public String getServerName() {
        return serverName;
    }

    public TileUrlTemplate getUrlTemplate() {
        return urlTemplate;
    }

    public double getTileWidth() {
        return tileWidth;
    }

    public double getTileHeight() {
        return tileHeight;
    }

    public int getMinZoomLevel() {
        return minZoomLevel;
    }

    public int getMaxZoomLevel() {
        return maxZoomLevel;
    }

    @Override
    public String toString() {
        return serverName;
    }
}
