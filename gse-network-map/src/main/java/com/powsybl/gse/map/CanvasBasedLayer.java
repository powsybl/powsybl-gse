/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapView;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CanvasBasedLayer<T> extends MapLayer {

    protected final Canvas canvas;

    protected RTree<T, Geometry> tree = RTree.star().maxChildren(6).create();

    protected CanvasBasedLayer(MapView mapView) {
        Objects.requireNonNull(mapView);
        canvas = new Canvas();
        canvas.widthProperty().bind(mapView.widthProperty());
        canvas.heightProperty().bind(mapView.heightProperty());
        getChildren().add(canvas);
    }

    protected Coordinate getMapCoordinate(Point2D point) {
        return getMapCoordinate(baseMap.zoom().get(), point);
    }

    protected Coordinate getMapCoordinate(double zoom, Point2D point) {
        double mex = point.getX() - baseMap.getTranslateX();
        double mey = point.getY() - baseMap.getTranslateY();
        double id = mex / 256;
        double jd = mey / 256;
        double n = Math.pow(2, zoom);
        double lon = id / n * 360.0 - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * jd / n)));
        double lat = latRad * 180.0 / Math.PI;
        return new Coordinate(lon, lat);
    }

    protected Rectangle getMapBounds() {
        Bounds bounds = baseMap.getParent().getLayoutBounds();
        Coordinate c1 = getMapCoordinate(new Point2D(bounds.getMinX(), bounds.getMaxY()));
        Coordinate c2 = getMapCoordinate(new Point2D(bounds.getMaxX(), bounds.getMinY()));
        return Geometries.rectangleGeographic(c1.getLon(), c1.getLat(), c2.getLon(), c2.getLat());
    }

    @Override
    protected void layoutLayer() {
        super.layoutLayer();

        // clear
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
