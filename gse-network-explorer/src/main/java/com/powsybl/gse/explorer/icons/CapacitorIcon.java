/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CapacitorIcon extends Pane {

    private final Line l1;
    private final Line l2;
    private final Line l3;
    private final Line l4;

    public CapacitorIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        l1 = new Line();
        l1.setStroke(stroke);
        l1.setStrokeWidth(strokeWidth);

        l2 = new Line();
        l2.setStroke(stroke);
        l2.setStrokeWidth(strokeWidth);

        l3 = new Line();
        l3.setStroke(stroke);
        l3.setStrokeWidth(strokeWidth);

        l4 = new Line();
        l4.setStroke(stroke);
        l4.setStrokeWidth(strokeWidth);

        getChildren().addAll(l1, l2, l3, l4);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        double marginX = 1d / 6;
        double marginY = 1d / 10;

        l1.setStartX(size / 2);
        l1.setStartY(size * marginY);
        l1.setEndX(size / 2);
        l1.setEndY(size * (1d / 2 - marginY));

        l2.setStartX(size * marginX);
        l2.setStartY(size * (1d / 2 - marginY));
        l2.setEndX(size * (1d - marginX));
        l2.setEndY(size * (1d / 2 - marginY));

        l3.setStartX(size * marginX);
        l3.setStartY(size * (1d / 2 + marginY));
        l3.setEndX(size * (1 - marginX));
        l3.setEndY(size * (1d / 2 + marginY));

        l4.setStartX(size / 2);
        l4.setStartY(size * (1d / 2 + marginY));
        l4.setEndX(size / 2);
        l4.setEndY(size * (1 - marginY));
    }
}
