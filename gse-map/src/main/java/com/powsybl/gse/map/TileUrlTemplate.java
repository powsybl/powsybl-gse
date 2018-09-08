package com.powsybl.gse.map;

/**
 * Tile URL template.
 * ${x} x tile
 * ${y} y tile
 * ${z} zoom level
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileUrlTemplate {

    private final String urlTemplate;

    public TileUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    String instanciate(TilePoint tilePoint) {
        return urlTemplate
                    .replace("${x}", Integer.toString((int) Math.floor(tilePoint.getX())))
                    .replace("${y}", Integer.toString((int) Math.floor(tilePoint.getY())))
                    .replace("${z}", Integer.toString(tilePoint.getZoom()));
    }
}
