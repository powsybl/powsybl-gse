/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.symbols;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GroundSymbol extends Pane {

    private final Line[] lines = new Line[4];

    public GroundSymbol(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        for (int i = 0; i < lines.length; i++) {
            lines[i] = new Line();
            lines[i].setStroke(stroke);
            lines[i].setStrokeWidth(strokeWidth);
        }
        getChildren().addAll(lines);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        lines[0].setStartX(size / 2);
        lines[0].setStartY(0);
        lines[0].setEndX(size / 2);
        lines[0].setEndY(size / 2);

        lines[1].setStartX(0);
        lines[1].setStartY(size / 2);
        lines[1].setEndX(size);
        lines[1].setEndY(size / 2);

        lines[2].setStartX(size / 4);
        lines[2].setStartY(size * 3 / 4);
        lines[2].setEndX(size * 3 / 4);
        lines[2].setEndY(size * 3 / 4);

        lines[3].setStartX(size * 3 / 8);
        lines[3].setStartY(size);
        lines[3].setEndX(size * 5 / 8);
        lines[3].setEndY(size);
    }
}
