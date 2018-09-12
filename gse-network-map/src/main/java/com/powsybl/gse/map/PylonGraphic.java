/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.gse.map.util.Coordinate;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PylonGraphic {

    public static class Neighbor {

        private final PylonGraphic pylon;

        private final SegmentGraphic segment;

        public Neighbor(PylonGraphic pylon, SegmentGraphic segment) {
            this.pylon = Objects.requireNonNull(pylon);
            this.segment = Objects.requireNonNull(segment);
        }

        public PylonGraphic getPylon() {
            return pylon;
        }

        public SegmentGraphic getSegment() {
            return segment;
        }
    }

    private final Coordinate coordinate;

    private final List<Neighbor> neighbors = new ArrayList<>();

    public PylonGraphic(Coordinate coordinate) {
        this.coordinate = Objects.requireNonNull(coordinate);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }
}
