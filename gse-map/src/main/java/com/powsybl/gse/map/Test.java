/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.gse.map.util.Coordinate;
import com.powsybl.gse.map.tile.TileManager;
import com.powsybl.gse.map.tile.TileServerInfo;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Test extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private TileManager tileManager;

    @Override
    public void init() {
        tileManager = new TileManager();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Map tile test");
        BorderPane root = new BorderPane();
        MapView mapView = new MapView(tileManager);
        mapView.addLayer(new MapLayer() {

            final Coordinate c = new Coordinate(2.162, 48.801);

            @Override
            public void init() {
            }

            @Override
            public void update(Canvas canvas, MapViewPort viewPort) {
                if (viewPort.getGeographicalBounds().contains(c)) {
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.setFill(Color.BLUE);
                    Point2D p = viewPort.getPoint(c);
                    gc.fillRect(p.getX(), p.getY(), 50, 50);
                }
            }
        });
        mapView.zoomProperty().set(13);
        mapView.centerProperty().set(new Coordinate(2.162, 48.801));
        root.setCenter(mapView);
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(event -> mapView.zoomProperty().set(mapView.zoomProperty().get() + 1));
        Button zoomOut = new Button("-");
        zoomOut.setOnAction(event -> mapView.zoomProperty().set(mapView.zoomProperty().get() - 1));
        ComboBox<TileServerInfo> serverNames = new ComboBox<>();
        serverNames.getItems().addAll(TileServerInfo.OSM_STANDARD,
                                      TileServerInfo.OPEN_CYCLE_MAP);
        serverNames.getSelectionModel().select(0);
        tileManager.serverInfoProperty().bind(serverNames.getSelectionModel().selectedItemProperty());
        ToolBar toolBar = new ToolBar(zoomIn, zoomOut, serverNames);
        root.setTop(toolBar);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    @Override
    public void stop() {
        tileManager.close();
    }
}
