package com.powsybl.gse.map;

import java.io.InputStream;
import java.util.Optional;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface TileImage {

    Optional<InputStream> getInputStream();
}
