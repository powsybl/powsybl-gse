/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.commons.util.ServiceLoaderCache;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface BrandingConfig {

    static BrandingConfig find() {
        List<BrandingExtension> extensions = new ServiceLoaderCache<>(BrandingExtension.class).getServices();
        if (extensions.isEmpty()) {
            return new DefaultBrandingConfig();
        }
        if (extensions.size() > 1) {
            throw new GseException("Only one branding extension is allowed");
        }
        return extensions.get(0).getConfig();
    }

    String getTitle();

    Image getIcon16x16();

    Region getLogo();

    Pane getAboutPane();

    Optional<GseAppDocumentation> getDocumentation(Application app);
}
