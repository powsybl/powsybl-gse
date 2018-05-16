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
import javafx.scene.shape.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class InductorIcon extends Pane {

    private final Line l1;
    private final Arc a1;
    private final Arc a2;
    private final Arc a3;
    private final Line l2;

    public InductorIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        l1 = new Line();
        l1.setStroke(stroke);
        l1.setStrokeWidth(strokeWidth);
        a1 = new Arc();
        a1.setStartAngle(90);
        a1.setLength(-180);
        a1.setFill(Color.TRANSPARENT);
        a1.setStroke(stroke);
        a1.setStrokeWidth(strokeWidth);
        a2 = new Arc();
        a2.setStartAngle(90);
        a2.setLength(-180);
        a2.setFill(Color.TRANSPARENT);
        a2.setStroke(stroke);
        a2.setStrokeWidth(strokeWidth);
        a3 = new Arc();
        a3.setStartAngle(90);
        a3.setLength(-180);
        a3.setFill(Color.TRANSPARENT);
        a3.setStroke(stroke);
        a3.setStrokeWidth(strokeWidth);
        l2 = new Line();
        l2.setStroke(stroke);
        l2.setStrokeWidth(strokeWidth);
        getChildren().addAll(l1, a1, a2, a3, l2);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        double marginY = size / 10;

        l1.setStartX(size / 2);
        l1.setStartY(0);
        l1.setEndX(size / 2);
        l1.setEndY(marginY);

        double r = (size - (2 * marginY)) / (2 * 3);

        a1.setCenterX(size / 2);
        a1.setCenterY(marginY + r);
        a1.setRadiusX(r);
        a1.setRadiusY(r);

        a2.setCenterX(size / 2);
        a2.setCenterY(marginY + 3 * r);
        a2.setRadiusX(r);
        a2.setRadiusY(r);

        a3.setCenterX(size / 2);
        a3.setCenterY(marginY + 5 * r);
        a3.setRadiusX(r);
        a3.setRadiusY(r);

        l2.setStartX(size / 2);
        l2.setStartY(size - marginY);
        l2.setEndX(size / 2);
        l2.setEndY(size);
    }
}
