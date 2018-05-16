/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.google.auto.service.AutoService;
import com.powsybl.gse.spi.FontLoader;
import javafx.scene.text.Font;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(FontLoader.class)
public class DefaultFontLoader implements FontLoader {

    @Override
    public void load() {
        Font.loadFont(DefaultFontLoader.class.getResourceAsStream("/fonts/fontawesome-webfont.ttf"), 12);
        Font.loadFont(DefaultFontLoader.class.getResourceAsStream("/fonts/zocial.ttf"), 12);
    }
}
