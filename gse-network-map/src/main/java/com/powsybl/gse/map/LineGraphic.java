package com.powsybl.gse.map;

import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineGraphic {

    static final Color[] NOMINAL_VOLTAGE_COLOR = {
        Color.rgb(171, 175, 40),  // jaune fonce pur etre lisible sur du blanc
        Color.rgb(255, 130, 144), // pink1   plus lisible
        Color.rgb(160, 32, 240),  // purple
        Color.rgb(204, 85, 0),    // orange
        Color.rgb(1, 175, 175),   //cyan  plus fonce  150 KV
        Color.rgb(34, 139, 34),   // forest green 225 KV
        Color.RED
    };

    private static final Pattern ANGLE_PATTERN = Pattern.compile("\"(.*)° (.*)' (.*)\"\"\"");

    private final String id;

    private final int drawOrder;

    private final Color color;

    private final Set<PylonGraphic> pylons = new TreeSet<>();

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

    public Set<PylonGraphic> getPylons() {
        return pylons;
    }

    private static double parseAngle(String angle) {
        Matcher matcher = ANGLE_PATTERN.matcher(angle);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid angle " + angle);
        }
        double degree = Double.parseDouble(matcher.group(1));
        double minutes = Double.parseDouble(matcher.group(2));
        double seconds = Double.parseDouble(matcher.group(3).replace(',', '.'));
        return degree + minutes / 60 + seconds / 3600;
    }

    static Color parseNominalVoltage(String nominalVoltage) {
        switch (nominalVoltage) {
            case "400 KV":
            case "400 kV":
                return NOMINAL_VOLTAGE_COLOR[6];
            case "225 KV":
            case "225 kV":
                return NOMINAL_VOLTAGE_COLOR[5];
            case "150 KV":
            case "150 kV":
                return NOMINAL_VOLTAGE_COLOR[4];
            case "90 KV":
            case "90 kV":
                return NOMINAL_VOLTAGE_COLOR[3];
            case "63 KV":
            case "63 kV":
                return NOMINAL_VOLTAGE_COLOR[2];
            case "45 KV":
            case "45 kV":
                return NOMINAL_VOLTAGE_COLOR[1];
            case "INFERIEUR A 45 KV":
            case "INF 45 kV":
                return NOMINAL_VOLTAGE_COLOR[0];
            case "HORS TENSION":
                return Color.BLACK;
            default:
                throw new AssertionError(nominalVoltage);
        }
    }

    private static int getDrawOrder(String nominalVoltage) {
        switch (nominalVoltage) {
            case "400 KV":
                return 0;
            case "225 KV":
                return 1;
            case "150 KV":
                return 2;
            case "90 KV":
                return 3;
            case "63 KV":
                return 4;
            case "45 KV":
                return 5;
            case "INFERIEUR A 45 KV":
                return 6;
            case "HORS TENSION":
                return 7;
            default:
                throw new AssertionError(nominalVoltage);
        }
    }

    public static Collection<LineGraphic> parse() {
        Map<String, LineGraphic> lines = new HashMap<>();

        int pylonCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("C:/Users/geoff_/Documents/Extract09042018_IL_Pylones.csv"))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                String lineId = tokens[1];
                if (lineId.isEmpty()) {
                    continue;
                }
                Color color = parseNominalVoltage(tokens[2]);
                int drawOrder = getDrawOrder(tokens[2]);
                int pylonNum = Integer.parseInt(tokens[8]);
                if ("Non-Renseignée".equals(tokens[23])) {
                    continue;
                }
                double lon = parseAngle(tokens[23]);
                double lat = parseAngle(tokens[24]);
                if (lat < 0) { // discard bad data
                    continue;
                }
                lines.computeIfAbsent(lineId, id -> new LineGraphic(id, drawOrder, color)).getPylons()
                        .add(new PylonGraphic(new Coordinate(lon, lat), pylonNum));
                pylonCount++;
            }

            System.out.println(lines.size() + "lines, " + pylonCount + " pylons");

//            for (Map.Entry<String, Set<PylonGraphic>> e : coords.entrySet()) {
//                System.out.println(e.getKey() + " " + e.getValue());
//            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return lines.values();
    }
}
