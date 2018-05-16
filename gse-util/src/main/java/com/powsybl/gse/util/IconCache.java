/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.gse.spi.GseException;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum IconCache {
    INSTANCE;

    private final Map<String, Image> imagesCache = new HashMap<>();

    public synchronized Image get(Class<?> clazz, String name) {
        Image image = imagesCache.get(name);
        if (image == null) {
            InputStream is = clazz.getResourceAsStream("/icons/" + name + ".png");
            if (is == null) {
                is = clazz.getResourceAsStream("/icons/" + name + ".gif");
            }
            if (is == null) {
                throw new GseException("Icon '" + name + "' not found");
            }
            image = new Image(is);
            imagesCache.put(name, image);
        }
        return image;
    }
}
