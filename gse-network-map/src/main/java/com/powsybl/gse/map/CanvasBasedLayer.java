/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CanvasBasedLayer extends MapLayer {

    protected final Canvas canvas;

    public CanvasBasedLayer(MapView mapView) {
        Objects.requireNonNull(mapView);
        canvas = new Canvas();
        canvas.widthProperty().bind(mapView.widthProperty());
        canvas.heightProperty().bind(mapView.heightProperty());
        getChildren().add(canvas);
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        // clear
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
