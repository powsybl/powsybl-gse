/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GeneratorIcon extends Pane {

    private final Circle c;
    private final Arc a1;
    private final Arc a2;

    public GeneratorIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        c = new Circle();
        c.setFill(Color.TRANSPARENT);
        c.setStroke(stroke);
        c.setStrokeWidth(strokeWidth);
        a1 = new Arc();
        a1.setStartAngle(0);
        a1.setLength(180);
        a1.setFill(Color.TRANSPARENT);
        a1.setStroke(stroke);
        a1.setStrokeWidth(strokeWidth);
        a2 = new Arc();
        a2.setStartAngle(0);
        a2.setLength(-180);
        a2.setFill(Color.TRANSPARENT);
        a2.setStroke(stroke);
        a2.setStrokeWidth(strokeWidth);
        getChildren().addAll(c, a1, a2);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight()) * 0.9;
        double margin = size / 10;

        c.setCenterX(size / 2);
        c.setCenterY(size / 2);
        c.setRadius(size / 2);

        a1.setCenterX(size / 4 + margin / 2);
        a1.setCenterY(size / 2);
        a1.setRadiusX(size / 4 - margin / 2);
        a1.setRadiusY(size / 4 - margin / 2);

        a2.setCenterX(size * 3 / 4 - margin / 2);
        a2.setCenterY(size / 2);
        a2.setRadiusX(size / 4 - margin / 2);
        a2.setRadiusY(size / 4 - margin / 2);
    }
}
