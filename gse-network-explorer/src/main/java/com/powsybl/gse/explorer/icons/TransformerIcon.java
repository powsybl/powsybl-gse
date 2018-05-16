/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TransformerIcon extends Pane {

    private final Circle c1;
    private final Circle c2;

    public TransformerIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        c1 = new Circle();
        c1.setFill(Color.TRANSPARENT);
        c1.setStroke(stroke);
        c1.setStrokeWidth(strokeWidth);

        c2 = new Circle();
        c2.setFill(Color.TRANSPARENT);
        c2.setStroke(stroke);
        c2.setStrokeWidth(strokeWidth);

        getChildren().addAll(c1, c2);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        double shift = size / 3.3;

        c1.setCenterX(size / 2);
        c1.setCenterY(shift);
        c1.setRadius(shift);

        c2.setCenterX(size / 2);
        c2.setCenterY(size - shift);
        c2.setRadius(shift);
    }
}
