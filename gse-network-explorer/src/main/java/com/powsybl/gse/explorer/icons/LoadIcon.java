/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LoadIcon extends Pane {

    private static final double ARROW_START = 0.7;
    private static final double MARGIN = 0.1;

    private final Polyline l;

    public LoadIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        l = new Polyline();
        l.setFill(stroke);
        l.setStroke(stroke);
        l.setStrokeWidth(strokeWidth);

        getChildren().addAll(l);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        l.getPoints().setAll(new Double[] {
            size / 2,
            size * MARGIN,
            size / 2,
            size * ARROW_START,
            size / 4,
            size * ARROW_START,
            size / 2,
            size * (1 - MARGIN),
            size * 3 / 4,
            size * ARROW_START,
            size / 2,
            size * ARROW_START
        });
    }
}
