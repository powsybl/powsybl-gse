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
import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;
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

    private static final int PYLON_SHOW_ZOOM_THRESHOLD = 10;

    private static final PointExtractor<Point2D> POINT_EXTRACTOR = new PointExtractor<Point2D>() {
        @Override
        public double getX(Point2D point2D) {
            return point2D.getX();
        }

        @Override
        public double getY(Point2D point2D) {
            return point2D.getY();
        }
    };

    private final Simplify<Point2D> simplify = new Simplify<>(new Point2D[0], POINT_EXTRACTOR);

    private final SortedMap<Integer, BranchGraphicIndex> branchesIndexes;

    private final CancellableGraphicTaskQueue taskQueue;

    private final NetworkMapConfig config;

    public LineDemoLayer(MapView mapView, SortedMap<Integer, BranchGraphicIndex> branchesIndexes,
                         CancellableGraphicTaskQueue taskQueue, NetworkMapConfig config) {
        super(mapView);
        this.branchesIndexes = Objects.requireNonNull(branchesIndexes);
        this.taskQueue = Objects.requireNonNull(taskQueue);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    protected void onMapClick(Coordinate c) {
        for (Map.Entry<Integer, BranchGraphicIndex> e : branchesIndexes.entrySet()) {
            BranchGraphicIndex index = e.getValue();
            Observable<Entry<BranchGraphic, Geometry>> search = index.getTree().search(Geometries.pointGeographic(c.getLon(), c.getLat()));
            search.toBlocking().forEach(new Action1<Entry<BranchGraphic, Geometry>>() {
                @Override
                public void call(Entry<BranchGraphic, Geometry> entry) {
                    System.out.println(entry.value().getLine().getId());
                }
            });
        }
    }

    private void draw(GraphicsContext gc, int drawOrder, BranchGraphicIndex segmentIndex,
                      double zoom, boolean showPylons) {
        LOGGER.trace("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int[] segmentCount = new int[1];
        int[] drawnSegmentCount = new int[1];

        double pylonSize = 5;

        segmentIndex.getTree().search(getMapBounds()).toBlocking().forEach(e -> {
            BranchGraphic segmentGroup = e.value();

            gc.setStroke(segmentGroup.getLine().getColor());
            gc.setFill(segmentGroup.getLine().getColor());

            Point2D[] points = new Point2D[segmentGroup.getPylons().size()];
            int i = 0;
            for (PylonGraphic pylon : segmentGroup.getPylons()) {
                Coordinate c = pylon.getCoordinate();
                Point2D point = baseMap.getMapPoint(c.getLat(), c.getLon());
                points[i++] = point;
            }

            Point2D[] pointsToDraw;
            if (zoom > PYLON_SHOW_ZOOM_THRESHOLD) {
                pointsToDraw = points;
            } else {
                pointsToDraw = simplify.simplify(points, 1, true);
            }

            segmentCount[0] += points.length - 1;
            drawnSegmentCount[0] += pointsToDraw.length - 1;

            Point2D prev = null;
            for (Point2D point : pointsToDraw) {
                if (prev != null) {
                    gc.strokeLine(prev.getX(), prev.getY(), point.getX(), point.getY());
                }
                // draw pylon
                if (showPylons && zoom > PYLON_SHOW_ZOOM_THRESHOLD) {
                    gc.fillArc(point.getX() - pylonSize / 2, point.getY() - pylonSize / 2, pylonSize, pylonSize, 0, 360, ArcType.ROUND);
                }
                prev = point;
            }
        });

        stopWatch.stop();

        double simplificationRate = 1;
        if (drawnSegmentCount[0] != 0) {
            simplificationRate = (double) drawnSegmentCount[0] / segmentCount[0];
        }

        LOGGER.info("{} line segments (order={}, simplification={}) drawn in {} ms at zoom {}",
                segmentCount[0], drawOrder, simplificationRate, stopWatch.getTime(), baseMap.zoom().getValue());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        double zoom = baseMap.zoom().getValue();
        boolean showPylons = config.isShowPylons().get();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(zoom >= 9 ? 2 : 1);

        Iterator<Map.Entry<Integer, BranchGraphicIndex>> it = branchesIndexes.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Integer, BranchGraphicIndex> e = it.next();
            draw(gc, e.getKey(), e.getValue(), zoom, showPylons);
        }

        while (it.hasNext()) {
            Map.Entry<Integer, BranchGraphicIndex> e = it.next();
            taskQueue.addTask(() -> draw(gc, e.getKey(), e.getValue(), zoom, showPylons));
        }
    }
}
