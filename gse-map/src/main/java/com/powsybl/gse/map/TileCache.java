package com.powsybl.gse.map;

import io.reactivex.Maybe;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
interface TileCache {

    Maybe<InputStream> readTile(Tile tile);

    OutputStream writeTile(Tile tile);
}
