/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
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
public class SubtationDemoLayer extends CanvasBasedLayer<SubstationGraphic> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubtationDemoLayer.class);

    private final Map<String, SubstationGraphic> substations;

    public SubtationDemoLayer(MapView mapView, Map<String, SubstationGraphic> substations) {
        super(mapView);
        this.substations = Objects.requireNonNull(substations);
    }

    @Override
    protected void initialize() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (Map.Entry<String, SubstationGraphic> e : substations.entrySet()) {
            SubstationGraphic substation = e.getValue();
            Point point = Geometries.pointGeographic(substation.getPosition().getLon(),
                                                     substation.getPosition().getLat());
            tree = tree.add(substation, point);
        }

        LOGGER.info("Substation R-tree built in {} ms", stopWatch.getTime());
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

        int[] drawnSubstations = new int[1];

        tree.search(getMapBounds())
                .toBlocking()
                .toIterable()
                .forEach(e -> {
                    SubstationGraphic substation = e.value();
                    Point2D p = baseMap.getMapPoint(substation.getPosition().getLat(),
                                                    substation.getPosition().getLon());
                    gc.setFill(substation.getColor());
                    gc.fillArc(p.getX() - size / 2, p.getY() - size / 2, size, size, 0, 360, ArcType.ROUND);
                    if (zoom > 9) {
                        gc.fillText(substation.getId(), p.getX() + 10, p.getY() + 10);
                    }
                    drawnSubstations[0]++;
                });

        stopWatch.stop();
        LOGGER.info("{} substations drawn in {} ms ", drawnSubstations[0], stopWatch.getTime());
    }
}
