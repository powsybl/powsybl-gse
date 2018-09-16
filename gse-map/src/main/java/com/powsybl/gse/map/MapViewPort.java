/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.gse.map.tile.TileManager;
import com.powsybl.gse.map.tile.TilePoint;
import com.powsybl.gse.map.util.Coordinate;
import com.powsybl.gse.map.util.GeographicalBounds;
import com.powsybl.gse.map.util.Polyline;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapViewPort {

    private final MapView mapView;

    private final GeographicalBounds geographicalBounds;

    private final TilePoint centerTilePoint;

    public MapViewPort(MapView mapView, GeographicalBounds geographicalBounds, TilePoint centerTilePoint) {
        this.mapView = Objects.requireNonNull(mapView);
        this.geographicalBounds = Objects.requireNonNull(geographicalBounds);
        this.centerTilePoint = Objects.requireNonNull(centerTilePoint);
    }

    public GeographicalBounds getGeographicalBounds() {
        return geographicalBounds;
    }

    public Point2D getPoint(Coordinate c) {
        Objects.requireNonNull(c);
        int zoom = mapView.zoomProperty().get();
        TileManager tileManager = mapView.getTileManager();
        TilePoint tilePoint = tileManager.project(c, zoom);
        return new Point2D(mapView.getWidth() / 2 + (tilePoint.getX() - centerTilePoint.getX()) * tileManager.getServerInfo().getTileWidth(),
                           mapView.getHeight() / 2 + (tilePoint.getY() - centerTilePoint.getY()) * tileManager.getServerInfo().getTileHeight());
    }

    public Polyline getPoints(List<Coordinate> coordinates) {
        Objects.requireNonNull(coordinates);
        int zoom = mapView.zoomProperty().get();
        TileManager tileManager = mapView.getTileManager();
        double[] x = new double[coordinates.size()];
        double[] y = new double[coordinates.size()];
        tileManager.project(coordinates, zoom, x, y);
        return new Polyline(x, y);
    }
}
