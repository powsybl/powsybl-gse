/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BadgeCount extends StackPane {

    private final Text text;

    public BadgeCount(int count) {
        text = new Text(Integer.toString(count));
        text.setStyle("-fx-fill: white");
        double width = text.getBoundsInLocal().getWidth();
        double height = text.getBoundsInLocal().getHeight();
        double x = getLayoutX() + getWidth() / 2;
        double y = getLayoutY() + getHeight() / 2;
        double radius = Math.max(width, height) / 2;
        Circle c1 = new Circle(x, y, radius * 1.3, Color.WHITE);
        Circle c2 = new Circle(x, y, radius * 1.2, Color.RED);
        getChildren().addAll(c1, c2, text);
    }

    public void setCount(int count) {
        text.setText(Integer.toString(count));
    }
}
