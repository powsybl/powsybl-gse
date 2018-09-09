package com.powsybl.gse.map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Test extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private TileSpace tileSpace;

    @Override
    public void init() {
        tileSpace = new TileSpace();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Map tile test");
        BorderPane root = new BorderPane();
        MapView mapView = new MapView(tileSpace);
        mapView.zoomProperty().set(13);
        mapView.centerProperty().set(new Coordinate(2.162, 48.801));
        root.setCenter(mapView);
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(event -> mapView.zoomProperty().set(mapView.zoomProperty().get() + 1));
        Button zoomOut = new Button("-");
        zoomOut.setOnAction(event -> mapView.zoomProperty().set(mapView.zoomProperty().get() - 1));
        ComboBox<TileServerInfo> serverNames = new ComboBox<>();
        serverNames.getItems().addAll(TileServerInfo.OSM_STANDARD, TileServerInfo.OPEN_CYCLE_MAP);
        ToolBar toolBar = new ToolBar(zoomIn, zoomOut, serverNames);
        root.setTop(toolBar);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    @Override
    public void stop() {
        tileSpace.close();
    }
}
