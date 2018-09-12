/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.powsybl.commons.PowsyblException;
import com.powsybl.gse.map.util.Coordinate;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BranchGraphic implements IndexableGraphic {

    private final List<PylonGraphic> pylons;

    private final LineGraphic line;

    public BranchGraphic(List<PylonGraphic> pylons, LineGraphic line) {
        this.pylons = Objects.requireNonNull(pylons);
        if (pylons.isEmpty()) {
            throw new PowsyblException("Empty poly segment");
        }
        this.line = Objects.requireNonNull(line);
    }

    public List<PylonGraphic> getPylons() {
        return pylons;
    }

    public LineGraphic getLine() {
        return line;
    }

    @Override
    public Rectangle getBoundingBox() {
        double minLon = Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double maxLat = Double.MIN_VALUE;
        for (PylonGraphic pylon : pylons) {
            Coordinate c = pylon.getCoordinate();
            minLon = Math.min(minLon, c.getLon());
            minLat = Math.min(minLat, c.getLat());
            maxLon = Math.max(maxLon, c.getLon());
            maxLat = Math.max(maxLat, c.getLat());
        }
        return Geometries.rectangleGeographic(minLon, minLat, maxLon, maxLat);
    }
}
