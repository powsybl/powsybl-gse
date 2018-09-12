/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.geometry.Rectangle;
import com.powsybl.gse.map.util.Coordinate;
import com.powsybl.iidm.network.Substation;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubstationGraphic implements IndexableGraphic {

    private final String id;

    private final Color color;

    private final Coordinate position;

    private Substation model;

    public SubstationGraphic(String id, Color color, Coordinate position) {
        this.id = Objects.requireNonNull(id);
        this.color = Objects.requireNonNull(color);
        this.position = Objects.requireNonNull(position);
    }

    public String getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public Coordinate getPosition() {
        return position;
    }

    public Substation getModel() {
        return model;
    }

    public void setModel(Substation model) {
        this.model = model;
    }

    @Override
    public Rectangle getBoundingBox() {
        throw new AssertionError("TODO");
    }

    @Override
    public String toString() {
        return id;
    }
}
