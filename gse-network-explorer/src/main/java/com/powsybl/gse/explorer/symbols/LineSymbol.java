/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.symbols;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineSymbol extends Pane {

    private final Polyline l;

    public LineSymbol(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        l = new Polyline();
        l.setStroke(stroke);
        l.setStrokeWidth(strokeWidth);

        getChildren().addAll(l);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        l.getPoints().setAll(new Double[] {
            size  / 4,
            0d,
            size / 4,
            size / 2,
            size * 3 / 4,
            size / 2,
            size * 3 / 4,
            size
        });
    }
}
