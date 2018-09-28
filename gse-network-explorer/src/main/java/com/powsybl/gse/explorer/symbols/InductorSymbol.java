/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.symbols;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Line;

import java.util.Arrays;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class InductorSymbol extends Pane {

    private final int spirals;

    private final Line l1;
    private final Arc[] a;
    private final Line l2;

    public InductorSymbol(Color stroke, double strokeWidth, double size) {
        this(stroke, strokeWidth, size, 3);
    }

    public InductorSymbol(Color stroke, double strokeWidth, double size, int spirals) {
        this.spirals = spirals;

        setPrefSize(size, size);

        l1 = new Line();
        l1.setStroke(stroke);
        l1.setStrokeWidth(strokeWidth);
        a = new Arc[spirals];
        for (int i = 0; i < spirals; i++) {
            a[i] = createArc(stroke, strokeWidth);
        }
        l2 = new Line();
        l2.setStroke(stroke);
        l2.setStrokeWidth(strokeWidth);
        getChildren().addAll(l1, l2);
        getChildren().addAll(Arrays.asList(a));
    }

    private static Arc createArc(Color stroke, double strokeWidth) {
        Arc a = new Arc();
        a.setStartAngle(135);
        a.setLength(-270);
        a.setFill(Color.TRANSPARENT);
        a.setStroke(stroke);
        a.setStrokeWidth(strokeWidth);
        return a;
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        double marginY = size / 10;

        l1.setStartX(size / 2);
        l1.setStartY(0);
        l1.setEndX(size / 2);
        l1.setEndY(marginY);

        double r = (size - (2 * marginY)) / (2 + 2 * (spirals - 1) * Math.cos(Math.PI / 4));

        for (int i = 0; i < spirals; i++) {
            a[i].setCenterX(size / 2);
            a[i].setCenterY(marginY + r + 2 * i * r * Math.cos(Math.PI / 4));
            a[i].setRadiusX(r);
            a[i].setRadiusY(r);
        }

        l2.setStartX(size / 2);
        l2.setStartY(size - marginY);
        l2.setEndX(size / 2);
        l2.setEndY(size);
    }
}
