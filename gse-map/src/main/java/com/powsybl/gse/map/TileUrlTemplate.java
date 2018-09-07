package com.powsybl.gse.map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class TileUrlTemplate {

    private final String urlTemplate;

    public TileUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    String instanciate(Tile tile) {
        return urlTemplate
                    .replace("${x}", Integer.toString(tile.getX()))
                    .replace("${y}", Integer.toString(tile.getY()))
                    .replace("${z}", Integer.toString(tile.getZoom()));
    }
}
