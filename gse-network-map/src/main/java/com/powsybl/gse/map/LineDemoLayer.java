package com.powsybl.gse.map;

import com.gluonhq.maps.MapView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineDemoLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineDemoLayer.class);

    private Map<Integer, List<LineGraphic>> lines;

    private CancellableGraphicTaskChain firstTask;

    public LineDemoLayer(MapView mapView) {
        super(mapView);
    }

    @Override
    protected void initialize() {
        lines = new TreeMap<>(RteOpenData.parseLines().stream().collect(Collectors.groupingBy(LineGraphic::getDrawOrder)));
    }

    private void draw(GraphicsContext gc, int drawOrder, List<LineGraphic> lines) {
        LOGGER.info("Drawing lines at order {}", drawOrder);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (LineGraphic line : lines) {

            gc.setStroke(line.getColor());

            for (SegmentGraphic segment : line.getSegments()) {
                Point2D point1 = baseMap.getMapPoint(segment.getCoordinate1().getLat(),
                                                     segment.getCoordinate1().getLon());
                Point2D point2 = baseMap.getMapPoint(segment.getCoordinate2().getLat(),
                                                     segment.getCoordinate2().getLon());

                gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
            }
        }

        stopWatch.stop();

        LOGGER.info("Lines drawn in {} ms at zoom {}", stopWatch.getTime(), baseMap.zoom().getValue());
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
        gc.setLineWidth(baseMap.zoom().getValue() >= 8 ? 2 : 1);

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
