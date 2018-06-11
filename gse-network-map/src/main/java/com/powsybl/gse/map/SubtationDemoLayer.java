/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.gluonhq.maps.MapView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubtationDemoLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubtationDemoLayer.class);

    private final Map<String, SubstationGraphic> substations;

    public SubtationDemoLayer(MapView mapView, Map<String, SubstationGraphic> substations) {
        super(mapView);
        this.substations = Objects.requireNonNull(substations);
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1);
        gc.setFont(Font.font(11));

        double zoom = baseMap.zoom().doubleValue();
        double size = zoom < 8 ? zoom / 2 : zoom;

        for (Map.Entry<String, SubstationGraphic> e : substations.entrySet()) {
            SubstationGraphic substation = e.getValue();
            Point2D p = baseMap.getMapPoint(substation.getPosition().getLat(),
                                            substation.getPosition().getLon());
            gc.setFill(substation.getColor());
            gc.fillArc(p.getX() - size / 2, p.getY() - size / 2, size, size, 0, 360, ArcType.ROUND);
            if (zoom > 9) {
                gc.fillText(substation.getId(), p.getX() + 10, p.getY() + 10);
            }
        }

        stopWatch.stop();
        LOGGER.info("Substations drawed in {} ms ", stopWatch.getTime());
    }
}
