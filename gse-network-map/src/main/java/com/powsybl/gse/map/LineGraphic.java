/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineGraphic {

    private final String id;

    private final int drawOrder;

    private final Color color;

    private final List<SegmentGraphic> segments = new ArrayList<>();

    public LineGraphic(String id, int drawOrder, Color color) {
        this.id = Objects.requireNonNull(id);
        this.drawOrder = drawOrder;
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
}
