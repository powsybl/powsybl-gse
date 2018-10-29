/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BadgeCount extends StackPane {

    private final Text text;
    private final Rectangle r1;
    private final Rectangle r2;

    public BadgeCount(int count) {
        text = new Text(Integer.toString(count));
        text.setStyle("-fx-fill: white");
        double height = text.getBoundsInLocal().getHeight();
        double width = Math.max(text.getBoundsInLocal().getWidth(), height);
        double x = getLayoutX() + getWidth() / 2;
        double y = getLayoutY() + getHeight() / 2;
        r1 = new Rectangle(x, y, width * 1.3, height * 1.3);
        r1.setFill(Color.WHITE);
        r1.setArcHeight(15);
        r1.setArcWidth(15);
        r2 = new Rectangle(x, y, width * 1.2, height * 1.2);
        r2.setFill(Color.RED);
        r2.setArcHeight(15);
        r2.setArcWidth(15);
        getChildren().addAll(r1, r2, text);
    }

    public void setCount(int count) {
        text.setText(Integer.toString(count));
        double height = text.getBoundsInLocal().getHeight();
        double width = Math.max(text.getBoundsInLocal().getWidth(), height);
        r1.setWidth(width * 1.3);
        r1.setHeight(height * 1.3);
        r2.setWidth(width * 1.2);
        r2.setHeight(height * 1.2);
    }
}
