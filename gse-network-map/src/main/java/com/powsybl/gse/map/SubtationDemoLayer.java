package com.powsybl.gse.map;

import com.gluonhq.maps.MapView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubtationDemoLayer extends CanvasBasedLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubtationDemoLayer.class);

    private final Map<String, SubstationGraphic> coords = new HashMap<>();

    public SubtationDemoLayer(MapView mapView) {
        super(mapView);
    }

    @Override
    protected void initialize() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("C:/Users/geoff_/Downloads/postes-electriques-rte-et-client.csv"))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                String id = tokens[0];
                Color color = LineGraphic.parseBaseVoltage(tokens[4]).getColor();
                double lon = Double.parseDouble(tokens[5]);
                double lat = Double.parseDouble(tokens[6]);
                coords.put(id, new SubstationGraphic(id, color, new Coordinate(lon, lat)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

        for (Map.Entry<String, SubstationGraphic> e : coords.entrySet()) {
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
