/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.gluonhq.maps.MapView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

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

    private final NetworkMapConfig config;

    public LineDemoLayer(MapView mapView, SortedMap<Integer, SegmentGraphicIndex> segmentIndexes, CancellableGraphicTaskQueue taskQueue,
                         NetworkMapConfig config) {
        super(mapView);
        this.segmentIndexes = Objects.requireNonNull(segmentIndexes);
        this.taskQueue = Objects.requireNonNull(taskQueue);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    protected void onMapClick(Coordinate c) {
        for (Map.Entry<Integer, SegmentGraphicIndex> e : segmentIndexes.entrySet()) {
            SegmentGraphicIndex index = e.getValue();
            Observable<Entry<SegmentGraphic, Geometry>> search = index.getTree().search(Geometries.pointGeographic(c.getLon(), c.getLat()));
            search.toBlocking().forEach(new Action1<Entry<SegmentGraphic, Geometry>>() {
                @Override
                public void call(Entry<SegmentGraphic, Geometry> entry) {
                    System.out.println(entry.value().getLine().getId());
                }
            });
        }
    }

    private void draw(GraphicsContext gc, int drawOrder, SegmentGraphicIndex segmentIndex,
                      double zoom, boolean showPylons) {
        LOGGER.trace("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int[] segmentCount = new int[1];

        double pylonSize = 5;

        segmentIndex.getTree().search(getMapBounds()).toBlocking().forEach(e -> {
            SegmentGraphic segment = e.value();

            gc.setStroke(segment.getLine().getColor());

            Coordinate c1 = segment.getCoordinate1();
            Coordinate c2 = segment.getCoordinate2();

            Point2D point1 = baseMap.getMapPoint(c1.getLat(), c1.getLon());
            Point2D point2 = baseMap.getMapPoint(c2.getLat(), c2.getLon());

            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());

            // draw pylon
            if (showPylons && zoom > 10) {
                gc.setFill(segment.getLine().getColor());
                gc.fillArc(point2.getX() - pylonSize / 2, point2.getY() - pylonSize / 2, pylonSize, pylonSize, 0, 360, ArcType.ROUND);
            }

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
        boolean showPylons = config.isShowPylons().get();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(zoom >= 9 ? 2 : 1);

        Iterator<Map.Entry<Integer, SegmentGraphicIndex>> it = segmentIndexes.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Integer, SegmentGraphicIndex> e = it.next();
            draw(gc, e.getKey(), e.getValue(), zoom, showPylons);
        }

        while (it.hasNext()) {
            Map.Entry<Integer, SegmentGraphicIndex> e = it.next();
            taskQueue.addTask(() -> draw(gc, e.getKey(), e.getValue(), zoom, showPylons));
        }
    }
}
