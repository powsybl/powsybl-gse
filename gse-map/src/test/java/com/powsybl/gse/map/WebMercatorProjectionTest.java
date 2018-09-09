package com.powsybl.gse.map;

import javafx.geometry.Point2D;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WebMercatorProjectionTest {

    @Test
    public void test() {
        Coordinate c = new Coordinate(2.162, 48.801);
        Point2D p = WebMercatorProjection.project(c);
        assertEquals(new Point2D(2.162, 0.9785243663814854), p);
        assertEquals(c, WebMercatorProjection.unproject(p));
    }
}
