package com.powsybl.gse.map;

import io.reactivex.Maybe;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
interface TileCache {

    Maybe<TileImage> readImage(Tile tile);
}
