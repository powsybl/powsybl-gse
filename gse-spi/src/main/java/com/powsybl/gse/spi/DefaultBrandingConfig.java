/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultBrandingConfig implements BrandingConfig {

    @Override
    public String getTitle() {
        return "Grid Study Environment";
    }

    public static Image createImage(Region node) {
        // WORKAROUND to convert a node to a JavaFX image
        // https://stackoverflow.com/questions/41029931/snapshot-image-cant-be-used-as-stage-icon
        int width = (int) node.getWidth();
        int height = (int) node.getHeight();
        BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        SwingFXUtils.fromFXImage(node.snapshot(snapshotParameters, new WritableImage(width, height)), bimg);
        return SwingFXUtils.toFXImage(bimg, new WritableImage(width, height));
    }

    @Override
    public List<Image> getIcons() {
        PowsyblLogo logo = new PowsyblLogo();
        logo.resize(128, 128);
        return Arrays.asList(createImage(logo));
    }

    @Override
    public Region getLogo() {
        PowsyblLogo logo = new PowsyblLogo();
        logo.setPrefSize(500, 500);
        return logo;
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
