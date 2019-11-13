/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.google.auto.service.AutoService;
import com.powsybl.gse.spi.StyleSheetLoader;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(StyleSheetLoader.class)
public class DefaultStyleSheetLoader implements StyleSheetLoader {

    @Override
    public void load(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/css/groovy-keywords.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/searchbar.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/fixes.css").toExternalForm());
    }
}
