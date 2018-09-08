package com.powsybl.gse.map;

import io.reactivex.Maybe;

import java.io.InputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
interface TileCache {

    Maybe<InputStream> readImage(Tile tile);
}
