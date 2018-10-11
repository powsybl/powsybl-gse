/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import afester.javafx.svg.SvgLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.io.InputStream;
import java.util.Collection;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractSvgImage extends Pane {

    private final double svgX;
    private final double svgY;
    private final double svgWidth;
    private final double svgHeight;

    public AbstractSvgImage(InputStream is, double svgX, double svgY, double svgWidth, double svgHeight) {
        this.svgX = svgX;
        this.svgY = svgY;
        this.svgWidth = svgWidth;
        this.svgHeight = svgHeight;
        Group svg = new SvgLoader().loadSvg(is);
        getChildren().addAll(extractNodes(svg));
    }

    protected abstract Collection<Node> extractNodes(Group svg);

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double scaleFactor = Math.min(getWidth() / svgWidth, getHeight() / svgHeight);
        Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0);
        Translate translate = new Translate(-svgX, -svgY);
        getTransforms().setAll(scale, translate);
    }
}
