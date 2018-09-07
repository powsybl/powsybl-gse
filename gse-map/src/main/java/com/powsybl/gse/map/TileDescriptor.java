package com.powsybl.gse.map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TileDescriptor {

    private final TileUrlTemplate urlTemplate;

    private final double width;

    private final double height;

    private final int minZoomLevel;

    private final int maxZoomLevel;

    public TileDescriptor(TileUrlTemplate urlTemplate, double width, double height, int minZoomLevel, int maxZoomLevel) {
        this.urlTemplate = urlTemplate;
        this.width = width;
        this.height = height;
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
    }

    public TileUrlTemplate getUrlTemplate() {
        return urlTemplate;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getMinZoomLevel() {
        return minZoomLevel;
    }

    public int getMaxZoomLevel() {
        return maxZoomLevel;
    }
}
