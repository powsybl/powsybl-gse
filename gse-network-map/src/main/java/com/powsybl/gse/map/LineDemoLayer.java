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
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineDemoLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineDemoLayer.class);

    private final SortedMap<Integer, SegmentGraphicIndex> segmentIndexes;

    private final CancellableGraphicTaskQueue taskQueue;

    public LineDemoLayer(MapView mapView, SortedMap<Integer, SegmentGraphicIndex> segmentIndexes, CancellableGraphicTaskQueue taskQueue) {
        super(mapView);
        this.segmentIndexes = Objects.requireNonNull(segmentIndexes);
        this.taskQueue = Objects.requireNonNull(taskQueue);
    }

    private void draw(GraphicsContext gc, int drawOrder, SegmentGraphicIndex segmentIndex) {
        LOGGER.trace("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int[] segmentCount = new int[1];

        segmentIndex.search(getMapBounds()).toBlocking().forEach(e -> {
            SegmentGraphic segment = e.value();

            gc.setStroke(segment.getLine().getColor());

            Point2D point1 = baseMap.getMapPoint(segment.getCoordinate1().getLat(),
                    segment.getCoordinate1().getLon());
            Point2D point2 = baseMap.getMapPoint(segment.getCoordinate2().getLat(),
                    segment.getCoordinate2().getLon());

            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());

            segmentCount[0]++;
        });

        stopWatch.stop();

        LOGGER.info("{} lines segment (order={}) drawn in {} ms at zoom {}",
                segmentCount[0], drawOrder, stopWatch.getTime(), baseMap.zoom().getValue());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        double zoom = baseMap.zoom().getValue();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(zoom >= 8 ? 2 : 1);

        Iterator<Map.Entry<Integer, SegmentGraphicIndex>> it = segmentIndexes.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Integer, SegmentGraphicIndex> e = it.next();
            draw(gc, e.getKey(), e.getValue());
        }

        while (it.hasNext()) {
            Map.Entry<Integer, SegmentGraphicIndex> e = it.next();
            taskQueue.addTask(() -> draw(gc, e.getKey(), e.getValue()));
        }
    }
}
