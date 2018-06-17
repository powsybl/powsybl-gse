/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.gluonhq.maps.MapView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineDemoLayer extends CanvasBasedLayer<SegmentGraphic> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineDemoLayer.class);

    private final Collection<LineGraphic> lines;

    private final CancellableGraphicTaskQueue taskQueue;

    public LineDemoLayer(MapView mapView, Collection<LineGraphic> lines, CancellableGraphicTaskQueue taskQueue) {
        super(mapView);
        this.lines = Objects.requireNonNull(lines);
        this.taskQueue = Objects.requireNonNull(taskQueue);
    }

    @Override
    protected void initialize() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (LineGraphic line : lines) {
            for (SegmentGraphic segment : line.getSegments()) {
                double lon1 = segment.getCoordinate1().getLon();
                double lat1 = segment.getCoordinate1().getLat();
                double lon2 = segment.getCoordinate2().getLon();
                double lat2 = segment.getCoordinate2().getLat();
                Rectangle rectangle = Geometries.rectangleGeographic(Math.min(lon1, lon2), Math.min(lat1, lat2),
                                                                     Math.max(lon1, lon2), Math.max(lat1, lat2));
                tree = tree.add(segment, rectangle);
            }
        }

        LOGGER.info("Line segments R-tree built in {} ms", stopWatch.getTime());
    }

    private void draw(GraphicsContext gc, int drawOrder, List<SegmentGraphic> segments) {
        LOGGER.trace("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (SegmentGraphic segment : segments) {
            gc.setStroke(segment.getLine().getColor());

            Point2D point1 = baseMap.getMapPoint(segment.getCoordinate1().getLat(),
                                                 segment.getCoordinate1().getLon());
            Point2D point2 = baseMap.getMapPoint(segment.getCoordinate2().getLat(),
                                                 segment.getCoordinate2().getLon());

            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
        }

        stopWatch.stop();

        LOGGER.info("{} lines segment (order={}) drawn in {} ms at zoom {}",
                segments.size(), drawOrder, stopWatch.getTime(), baseMap.zoom().getValue());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        double zoom = baseMap.zoom().getValue();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(zoom >= 8 ? 2 : 1);

        Map<Integer, List<SegmentGraphic>> orderedSegments = new TreeMap<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int[] segmentCount = new int[1];

        if (zoom > 7) {
            tree.search(getMapBounds())
                    .toBlocking()
                    .forEach(e -> {
                        SegmentGraphic segment = e.value();
                        orderedSegments.computeIfAbsent(segment.getLine().getDrawOrder(), k -> new ArrayList<>())
                                .add(segment);
                        segmentCount[0]++;
                    });
        } else {
            for (LineGraphic line : lines) {
                for (SegmentGraphic segment : line.getSegments()) {
                    orderedSegments.computeIfAbsent(segment.getLine().getDrawOrder(), k -> new ArrayList<>())
                            .add(segment);
                    segmentCount[0]++;
                }
            }
        }

        LOGGER.info("{} lines segment search in {} ms", segmentCount[0], stopWatch.getTime());

        Iterator<Map.Entry<Integer, List<SegmentGraphic>>> it = orderedSegments.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Integer, List<SegmentGraphic>> e = it.next();
            draw(gc, e.getKey(), e.getValue());
        }

        while (it.hasNext()) {
            Map.Entry<Integer, List<SegmentGraphic>> e = it.next();
            taskQueue.addTask(() -> draw(gc, e.getKey(), e.getValue()));
        }
    }
}
