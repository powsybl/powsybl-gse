/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

    String instanciate(Tile tile) {
        return urlTemplate
                    .replace("${x}", Integer.toString(tile.getX()))
                    .replace("${y}", Integer.toString(tile.getY()))
                    .replace("${z}", Integer.toString(tile.getZoom()));
    }
}
