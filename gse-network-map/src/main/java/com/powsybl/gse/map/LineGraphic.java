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
}
