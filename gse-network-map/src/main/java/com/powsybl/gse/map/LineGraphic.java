package com.powsybl.gse.map;

import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineGraphic {

    enum BaseVoltage {
        VL_400_KV(Color.RED, 0),
        VL_225_KV(Color.rgb(34, 139, 34), 1),
        VL_150_KV(Color.rgb(1, 175, 175), 2),
        VL_90_KV(Color.rgb(204, 85, 0), 3),
        VL_63_KV(Color.rgb(160, 32, 240), 4),
        VL_45_KV(Color.rgb(255, 130, 144), 5),
        VL_INF_45_KV(Color.rgb(171, 175, 40), 6),
        VL_HORS_TENSION(Color.BLACK, 7),
        VL_COURANT_CONTINU(Color.YELLOW, 8);

        private final Color color;

        private final int order;

        BaseVoltage(Color color, int order) {
            this.color = Objects.requireNonNull(color);
            this.order = order;
        }

        public Color getColor() {
            return color;
        }

        public int getOrder() {
            return order;
        }
    }

    private final String id;

    private final int drawOrder;

    private final Color color;

    private final List<SegmentGraphic> segments = new ArrayList<>();

    public LineGraphic(String id, int drawOrder, Color color) {
        this.id = Objects.requireNonNull(id);
        this.drawOrder = Objects.requireNonNull(drawOrder);
        this.color = Objects.requireNonNull(color);
    }

    public String getId() {
        return id;
    }

    public int getDrawOrder() {
        return drawOrder;
    }

    public Color getColor() {
        return color;
    }

    public List<SegmentGraphic> getSegments() {
        return segments;
    }

    static BaseVoltage parseBaseVoltage(String str) {
        switch (str) {
            case "400 KV":
            case "400 kV":
                return BaseVoltage.VL_400_KV;
            case "225 KV":
            case "225 kV":
                return BaseVoltage.VL_225_KV;
            case "150 KV":
            case "150 kV":
                return BaseVoltage.VL_150_KV;
            case "90 KV":
            case "90 kV":
                return BaseVoltage.VL_90_KV;
            case "63 KV":
            case "63 kV":
                return BaseVoltage.VL_63_KV;
            case "45 KV":
            case "45 kV":
                return BaseVoltage.VL_45_KV;
            case "INFERIEUR A 45 KV":
            case "INF 45 kV":
                return BaseVoltage.VL_INF_45_KV;
            case "HORS TENSION":
                return BaseVoltage.VL_HORS_TENSION;
            case "COURANT CONTINU":
                return BaseVoltage.VL_COURANT_CONTINU;
            default:
                throw new AssertionError(str);
        }
    }

    public static Collection<LineGraphic> parse() {
        Map<String, LineGraphic> lines = new HashMap<>();

        int segmentCount = 0;
        try {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get("C:/Users/geoff_/Documents/lignes-aeriennes.csv"))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(";");
                    String lineId = tokens[1];
                    if (lineId.isEmpty()) {
                        continue;
                    }
                    BaseVoltage baseVoltage = parseBaseVoltage(tokens[5]);
                    double lon1 = Double.parseDouble(tokens[8]);
                    double lat1 = Double.parseDouble(tokens[9]);
                    double lon2 = Double.parseDouble(tokens[10]);
                    double lat2 = Double.parseDouble(tokens[11]);
                    lines.computeIfAbsent(lineId, id -> new LineGraphic(id, baseVoltage.getOrder(), baseVoltage.getColor()))
                            .getSegments()
                            .add(new SegmentGraphic(new Coordinate(lon1, lat1), new Coordinate(lon2, lat2)));
                    segmentCount++;
                }
            }

            try (BufferedReader reader = Files.newBufferedReader(Paths.get("C:/Users/geoff_/Documents/lignes-souterraines.csv"))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(";");
                    String lineId = tokens[1];
                    if (lineId.isEmpty()) {
                        continue;
                    }
                    BaseVoltage baseVoltage = parseBaseVoltage(tokens[5]);
                    double lon1 = Double.parseDouble(tokens[9]);
                    double lat1 = Double.parseDouble(tokens[10]);
                    double lon2 = Double.parseDouble(tokens[11]);
                    double lat2 = Double.parseDouble(tokens[12]);
                    lines.computeIfAbsent(lineId, id -> new LineGraphic(id, baseVoltage.getOrder(), baseVoltage.getColor()))
                            .getSegments()
                            .add(new SegmentGraphic(new Coordinate(lon1, lat1), new Coordinate(lon2, lat2)));
                    segmentCount++;
                }
            }

            System.out.println(lines.size() + "lines, " + segmentCount + " segments");

//            for (Map.Entry<String, LineGraphic> e : lines.entrySet()) {
//                System.out.println(e.getKey() + " " + e.getValue().getSegments());
//            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return lines.values();
    }
}
