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
import com.powsybl.iidm.network.Line;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineLayer.class);

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

    private static final Simplify<Point2D> SIMPLIFY = new Simplify<>(new Point2D[0], POINT_EXTRACTOR);

    static class SimplificationStatistics {

        int segmentCount = 0;
        int drawnSegmentCount = 0;

        double getSimplificationRate() {
            if (drawnSegmentCount != 0) {
                return (double) drawnSegmentCount / segmentCount;
            } else {
                return 1;
            }
        }
    }

    public static class PointsToDraw {

        final Point2D[] array;

        final Color stroke;

        final Color fill;

        final boolean reverse;

        public PointsToDraw(Point2D[] array, Color stroke, Color fill, boolean reverse) {
            this.array = array;
            this.stroke = stroke;
            this.fill = fill;
            this.reverse = reverse;
        }
    }

    private final SortedMap<Integer, BranchGraphicIndex> branchesIndexes;

    private final CancellableGraphicTaskQueue taskQueue;

    private final MapTimer timer;

    private final NetworkMapConfig config;

    private final Canvas slowArrowsCanvas;

    private final Canvas mediumArrowsCanvas;

    private final Canvas fastArrowsCanvas;

    private final List<PointsToDraw> slowArrowsPointsList = new ArrayList<>();

    private final List<PointsToDraw> mediumArrowsPointsList = new ArrayList<>();

    private final List<PointsToDraw> fastArrowsPointsList = new ArrayList<>();

    private final ChangeListener<Number> slowArrowsListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            drawArrows(slowArrowsCanvas, slowArrowsPointsList, newValue.doubleValue());
        }
    };

    private final ChangeListener<Number> mediumArrowsListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            drawArrows(mediumArrowsCanvas, mediumArrowsPointsList, newValue.doubleValue());
        }
    };

    private final ChangeListener<Number> fastArrowsListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            drawArrows(fastArrowsCanvas, fastArrowsPointsList, newValue.doubleValue());
        }
    };

    public LineLayer(MapView mapView, SortedMap<Integer, BranchGraphicIndex> branchesIndexes,
                     CancellableGraphicTaskQueue taskQueue, MapTimer timer,
                     NetworkMapConfig config) {
        super(mapView);
        this.branchesIndexes = Objects.requireNonNull(branchesIndexes);
        this.taskQueue = Objects.requireNonNull(taskQueue);
        this.timer = Objects.requireNonNull(timer);
        this.config = Objects.requireNonNull(config);
        slowArrowsCanvas = createCanvas();
        mediumArrowsCanvas = createCanvas();
        fastArrowsCanvas = createCanvas();
        timer.getSlowProgress().addListener(slowArrowsListener);
        timer.getMediumProgress().addListener(mediumArrowsListener);
        timer.getFastProgress().addListener(fastArrowsListener);
    }

    @Override
    protected void onMapClick(Coordinate c) {
        for (Map.Entry<Integer, BranchGraphicIndex> e : branchesIndexes.entrySet()) {
            BranchGraphicIndex index = e.getValue();
            Observable<Entry<BranchGraphic, Geometry>> search = index.getTree().search(Geometries.pointGeographic(c.getLon(), c.getLat()));
            search.toBlocking().forEach(new Action1<Entry<BranchGraphic, Geometry>>() {
                @Override
                public void call(Entry<BranchGraphic, Geometry> entry) {
                    LOGGER.trace(entry.value().getLine().getId());
                }
            });
        }
    }

    private PointsToDraw computePointsToDraw(BranchGraphic branch, double zoom, SimplificationStatistics stats) {
        Point2D[] points = new Point2D[branch.getPylons().size()];
        int i = 0;
        for (PylonGraphic pylon : branch.getPylons()) {
            Coordinate c = pylon.getCoordinate();
            Point2D point = baseMap.getMapPoint(c.getLat(), c.getLon());
            points[i++] = point;
        }

        Point2D[] pointsToDraw;
        if (zoom > PYLON_SHOW_ZOOM_THRESHOLD) {
            pointsToDraw = points;
        } else {
            pointsToDraw = SIMPLIFY.simplify(points, 1, true);
        }

        Line l = branch.getLine().getModel();
        boolean reverse = l != null && l.getTerminal1().getP() >= 0;

        stats.segmentCount += points.length - 1;
        stats.drawnSegmentCount += pointsToDraw.length - 1;


        return new PointsToDraw(pointsToDraw, branch.getLine().getColor(), branch.getLine().getColor(), reverse);
    }

    private static final double arrowsSpacing = 50; // spacing between 2 arrows
    private static final double arrowBaseSize = 5;
    private static final double arrowHeadSize = 10;

    private void drawArrows(Canvas canvas, List<PointsToDraw> pointsList, double progress) {
        cleanCanvas(canvas);
        ArrowDrawingContext context = new ArrowDrawingContext();
        for (PointsToDraw points : pointsList) {
            drawArrows(slowArrowsCanvas.getGraphicsContext2D(), points, context, progress);
        }
    }

    static class ArrowDrawingContext {
        double lastSegmentDistance;
        double residualSpacing;
    }

    private static void drawArrows(GraphicsContext g, Point2D point1, Point2D point2, ArrowDrawingContext context) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();
        double dxSegment = x2 - x1;
        double dySegment = y2 - y1;
        double segmentLength = Math.hypot(dxSegment, dySegment);
        double a = Math.atan2(dySegment, dxSegment);
        double ap = Math.PI / 2 - a;
        double dxArrowBase = Math.cos(ap) * arrowBaseSize;
        double dyArrowBase = Math.sin(ap) * arrowBaseSize;
        double dxArrowHead = Math.cos(a) * arrowHeadSize;
        double dyArrowHead = Math.sin(a) * arrowHeadSize;
        double distance = context.lastSegmentDistance + context.residualSpacing;
        while (distance < context.lastSegmentDistance + segmentLength) {
            double xArrow = x1 + Math.cos(a) * (distance - context.lastSegmentDistance);
            double yArrow = y1 + Math.sin(a) * (distance - context.lastSegmentDistance);

            // draw arrow
            g.beginPath();
            g.moveTo(xArrow - dxArrowBase, yArrow + dyArrowBase);
            g.lineTo(xArrow + dxArrowBase, yArrow - dyArrowBase);
            g.lineTo(xArrow + dxArrowHead, yArrow + dyArrowHead);
            g.closePath();
            g.fill();

            distance += arrowsSpacing;
        }
        context.residualSpacing = distance - context.lastSegmentDistance - segmentLength;
        context.lastSegmentDistance += segmentLength;
    }

    public static void drawArrows(GraphicsContext g, PointsToDraw points, ArrowDrawingContext context, double progress) {
        g.setFill(points.fill);

        context.lastSegmentDistance = 0;
        context.residualSpacing = progress * arrowsSpacing;
        if (points.reverse) {
            for (int i = points.array.length - 1;  i >= 1; i--) {
                drawArrows(g, points.array[i], points.array[i - 1], context);
            }
        } else {
            for (int i = 1; i < points.array.length; i++) {
                drawArrows(g, points.array[i -1], points.array[i], context);
            }
        }
    }

    private void draw(GraphicsContext gc, int drawOrder, BranchGraphicIndex branchIndex,
                      double zoom, boolean showPylons) {
        LOGGER.trace("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        SimplificationStatistics stats = new SimplificationStatistics();

        double pylonSize = 5;

        branchIndex.getTree().search(getMapBounds()).toBlocking().forEach(e -> {
            BranchGraphic branch = e.value();

            PointsToDraw points = computePointsToDraw(branch, zoom, stats);

            gc.setStroke(points.stroke);
            gc.setFill(points.fill);

            Point2D prev = null;
            for (Point2D point : points.array) {
                // draw segment
                if (prev != null) {
                    gc.strokeLine(prev.getX(), prev.getY(), point.getX(), point.getY());
                }

                // draw pylon
                if (showPylons && zoom > PYLON_SHOW_ZOOM_THRESHOLD) {
                    gc.fillArc(point.getX() - pylonSize / 2, point.getY() - pylonSize / 2, pylonSize, pylonSize, 0, 360, ArcType.ROUND);
                }

                prev = point;
            }

            // draw flows
            Line l = branch.getLine().getModel();
            if (l != null &&
                    !Double.isNaN(l.getTerminal1().getI()) &&
                    l.getCurrentLimits1() != null &&
                    !Double.isNaN(l.getCurrentLimits1().getPermanentLimit())) {
                double rate = l.getTerminal1().getI() / l.getCurrentLimits1().getPermanentLimit();
                if (rate < 0.2) {
                    slowArrowsPointsList.add(points);
                } else if (rate < 0.5) {
                    mediumArrowsPointsList.add(points);
                } else {
                    fastArrowsPointsList.add(points);
                }
            }
        });

        stopWatch.stop();

        LOGGER.info("Speed: {} branches slow, {} branches medium, {} branches fast",
                slowArrowsPointsList.size(), mediumArrowsPointsList.size(), fastArrowsPointsList.size());

        LOGGER.info("{} line segments (order={}, simplification={}) drawn in {} ms at zoom {}",
                stats.segmentCount, drawOrder, stats.getSimplificationRate(), stopWatch.getTime(),
                baseMap.zoom().getValue());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        double zoom = baseMap.zoom().getValue();
        boolean showPylons = config.isShowPylons().get();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(zoom >= 9 ? 2 : 1);

        slowArrowsPointsList.clear();
        mediumArrowsPointsList.clear();
        fastArrowsPointsList.clear();

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

    @Override
    protected void dispose() {
        timer.getSlowProgress().removeListener(slowArrowsListener);
        timer.getMediumProgress().removeListener(mediumArrowsListener);
        timer.getFastProgress().removeListener(fastArrowsListener);
    }
}
