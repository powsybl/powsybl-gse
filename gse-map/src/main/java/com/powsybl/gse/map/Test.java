package com.powsybl.gse.map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
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
        StackPane root = new StackPane();
        MapView mapView = new MapView(tileSpace);
        mapView.zoomProperty().set(13);
        mapView.centerProperty().set(new Coordinate(2.162, 48.801));
        root.getChildren().add(mapView);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    @Override
    public void stop() {
        tileSpace.close();
    }
}
