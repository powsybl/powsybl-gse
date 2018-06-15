/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.PlatformConfig;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Parse RTE substation and line segment coordinates.
 * The 3 following file have to be downloaded in $HOME/.itools directory:
 * <ul>
 *     <li>
 *         <a href="https://rte-opendata.opendatasoft.com/explore/dataset/postes-electriques-rte-et-client/download/?format=csv&timezone=Europe/Berlin&use_labels_for_header=true">postes-electriques-rte-et-client.csv</a>
 *     </li>
 *     <li>
 *         <a href="https://rte-opendata.opendatasoft.com/explore/dataset/lignes-aeriennes/download/?format=csv&timezone=Europe/Berlin&use_labels_for_header=true">lignes-aeriennes.csv</a>
 *     </li>
 *     <li>
 *         <a href="https://rte-opendata.opendatasoft.com/explore/dataset/lignes-souterraines/download/?format=csv&timezone=Europe/Berlin&use_labels_for_header=true">lignes-souterraines.csv</a>
 *     </li>
 * </ul>
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class RteOpenData {

    private static final Logger LOGGER = LoggerFactory.getLogger(RteOpenData.class);

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

    private RteOpenData() {
    }

    private static BaseVoltage parseBaseVoltage(String str) {
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

    private static void skipHeader(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new PowsyblException("Header is missing");
        }
    }

    public static Map<String, SubstationGraphic> parseSubstations() {
        Map<String, SubstationGraphic> coords = new HashMap<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int substationCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(PlatformConfig.defaultConfig().getConfigDir().resolve("postes-electriques-rte-et-client.csv"))) {
            skipHeader(reader);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                String id = tokens[0];
                Color color = parseBaseVoltage(tokens[4]).getColor();
                double lon = Double.parseDouble(tokens[5]);
                double lat = Double.parseDouble(tokens[6]);
                coords.put(id, new SubstationGraphic(id, color, new Coordinate(lon, lat)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        LOGGER.info("{} substations read in {} ms", substationCount, stopWatch.getTime());

        return coords;
    }

    private static int parseLine(Map<String, LineGraphic> lines, String fileName, int lon1Index, int lat1Index, int lon2Index, int lat2Index) {
        int segmentCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(PlatformConfig.defaultConfig().getConfigDir().resolve(fileName))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                String lineId = tokens[1];
                if (lineId.isEmpty()) {
                    continue;
                }
                BaseVoltage baseVoltage = parseBaseVoltage(tokens[5]);
                double lon1 = Double.parseDouble(tokens[lon1Index]);
                double lat1 = Double.parseDouble(tokens[lat1Index]);
                double lon2 = Double.parseDouble(tokens[lon2Index]);
                double lat2 = Double.parseDouble(tokens[lat2Index]);
                LineGraphic lineGraphic  = lines.get(lineId);
                if (lineGraphic == null) {
                    lineGraphic = new LineGraphic(lineId, baseVoltage.getOrder(), baseVoltage.getColor());
                    lines.put(lineId, lineGraphic);
                }
                lineGraphic.getSegments().add(new SegmentGraphic(new Coordinate(lon1, lat1), new Coordinate(lon2, lat2), lineGraphic));
                segmentCount++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return segmentCount;
    }

    public static Collection<LineGraphic> parseLines() {
        Map<String, LineGraphic> lines = new HashMap<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int segmentCount = parseLine(lines, "lignes-aeriennes.csv", 8, 9, 10, 11);
        segmentCount += parseLine(lines, "lignes-souterraines.csv", 9, 10, 11, 12);

        LOGGER.info("{} lines, {} segments read in {} ms", lines.size(), segmentCount, stopWatch.getTime());

        return lines.values();
    }
}
