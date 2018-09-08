package com.powsybl.gse.map;

import io.reactivex.Maybe;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class LocalFileSystemTileCache implements TileCache {

    @Override
    public Maybe<TileImage> getImage(TilePoint tilePoint) {
        return Maybe.empty();
    }
}
