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
import javafx.scene.shape.Rectangle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SwitchIcon extends Pane {

    private static final double BOX_MARGIN = 0.3;
    private static final double MARGIN = 0.1;

    private final Line leg1;
    private final Line leg2;
    private final Rectangle box;

    public SwitchIcon(Color stroke, double strokeWidth, double size) {
        setPrefSize(size, size);

        leg1 = new Line();
        leg1.setFill(stroke);
        leg1.setStroke(stroke);
        leg1.setStrokeWidth(strokeWidth);

        leg2 = new Line();
        leg2.setFill(stroke);
        leg2.setStroke(stroke);
        leg2.setStrokeWidth(strokeWidth);

        box = new Rectangle();
        box.setFill(Color.TRANSPARENT);
        box.setStroke(stroke);
        box.setStrokeWidth(strokeWidth);

        getChildren().addAll(leg1, leg2, box);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        leg1.setStartX(size / 2);
        leg1.setStartY(size * MARGIN);
        leg1.setEndX(size / 2);
        leg1.setEndY(size * BOX_MARGIN);

        leg2.setStartX(size / 2);
        leg2.setStartY(size * (1 - MARGIN));
        leg2.setEndX(size / 2);
        leg2.setEndY(size * (1 - BOX_MARGIN));

        box.setX(size * BOX_MARGIN);
        box.setY(size * BOX_MARGIN);
        box.setWidth(size * (1 - 2 * BOX_MARGIN));
        box.setHeight(size * (1 - 2 * BOX_MARGIN));
    }
}
