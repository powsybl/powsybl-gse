package com.powsybl.gse.map;

import com.gluonhq.maps.MapView;
import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineDemoLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineDemoLayer.class);

    private static final boolean SIMPLIFICATION = false;

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

    private Map<Integer, List<LineGraphic>> lines;

    private CancellableGraphicTaskChain firstTask;

    public LineDemoLayer(MapView mapView) {
        super(mapView);
    }

    @Override
    protected void initialize() {
        lines = new TreeMap<>(LineGraphic.parse().stream().collect(Collectors.groupingBy(LineGraphic::getDrawOrder)));
    }

    private Point2D[] projectCoordinates(Set<PylonGraphic> pylons) {
        Point2D[] mapPoints = new Point2D[pylons.size()];
        int i = 0;
        for (PylonGraphic pylon : pylons) {
            mapPoints[i] = baseMap.getMapPoint(pylon.getCoordinate().getLat(),
                                               pylon.getCoordinate().getLon());
            i++;
        }
        return mapPoints;
    }

    private static Point2D[] simplifyMapPoints(Point2D[] mapPoints, Simplify<Point2D> simplify) {
        return SIMPLIFICATION ? simplify.simplify(mapPoints, 1f, false) : mapPoints;
    }

    private void draw(GraphicsContext gc, int drawOrder, List<LineGraphic> lines) {
        LOGGER.info("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int mapPointCount = 0;
        int simplifiedMapPointCount = 0;

        for (LineGraphic l : lines) {
            if (l.getPylons().size() < 2) {
                continue;
            }

            Point2D[] mapPoints = projectCoordinates(l.getPylons());
            Point2D[] simplifiedMapPoints = simplifyMapPoints(mapPoints, simplify);

            mapPointCount += mapPoints.length;
            simplifiedMapPointCount += simplifiedMapPoints.length;

            gc.setStroke(l.getColor());

            Point2D prev = null;
            for (Point2D mapPoint : simplifiedMapPoints) {
                if (prev != null) {
                    gc.strokeLine(prev.getX(), prev.getY(), mapPoint.getX(), mapPoint.getY());
                }
                prev = mapPoint;
            }
        }

        stopWatch.stop();
        LOGGER.info("Average simplification {}", ((float) simplifiedMapPointCount) / mapPointCount);
        LOGGER.info("Lines drawn in {} ms", stopWatch.getTime());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        if (firstTask != null) {
            firstTask.cancel();
            firstTask.waitForCompletion();
            firstTask = null;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1);

        Iterator<Map.Entry<Integer, List<LineGraphic>>> it = lines.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Integer, List<LineGraphic>> e = it.next();
            draw(gc, e.getKey(), e.getValue());
        }

        CancellableGraphicTaskChain prevTask = null;
        while (it.hasNext()) {
            Map.Entry<Integer, List<LineGraphic>> e = it.next();

            CancellableGraphicTaskChain nextTask = new CancellableGraphicTaskChain(() -> draw(gc, e.getKey(), e.getValue()));
            if (firstTask == null) {
                firstTask = nextTask;
            }
            if (prevTask != null) {
                prevTask.setNext(nextTask);
            }
            prevTask = nextTask;
        }

        if (firstTask != null) {
            firstTask.start();
        }
    }
}
