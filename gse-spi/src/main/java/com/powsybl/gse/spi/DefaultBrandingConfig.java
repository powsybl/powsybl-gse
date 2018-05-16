/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultBrandingConfig implements BrandingConfig {

    @Override
    public String getTitle() {
        return "Grid Study Environment";
    }

    @Override
    public Image getIcon16x16() {
        return new Image(DefaultBrandingConfig.class.getResourceAsStream("/images/powsybl-logo-16x16.png"));
    }

    @Override
    public Region getLogo() {
        return new PowsyblLogo(500, 500);
    }

    @Override
    public Pane getAboutPane() {
        return new DefaultAboutPane();
    }

    @Override
    public Optional<GseAppDocumentation> getDocumentation(Application app) {
        return Optional.empty();
    }
}
