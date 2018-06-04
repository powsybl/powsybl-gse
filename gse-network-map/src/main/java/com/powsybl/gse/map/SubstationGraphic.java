package com.powsybl.gse.map;

import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubstationGraphic {

    private final String id;

    private final Color color;

    private final Coordinate position;

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
}
