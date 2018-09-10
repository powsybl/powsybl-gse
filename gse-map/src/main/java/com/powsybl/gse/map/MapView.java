package com.powsybl.gse.map;

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapView extends Region {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapView.class);

    private final TileManager tileManager;

    private final Canvas tileCanvas = new Canvas();

    private final ObjectProperty<Coordinate> center = new SimpleObjectProperty<>(new Coordinate(0, 0));

    private final IntegerProperty zoom;

    public MapView(TileManager tileManager) {
        this.tileManager = Objects.requireNonNull(tileManager);
        // bounded property to limit zoom level
        zoom = new SimpleIntegerProperty(tileManager.getServerInfo().getMinZoomLevel()) {
            @Override
            public void set(int newValue) {
                if (newValue < tileManager.getServerInfo().getMinZoomLevel()) {
                    super.set(tileManager.getServerInfo().getMinZoomLevel());
                } else if (newValue > tileManager.getServerInfo().getMaxZoomLevel()) {
                    super.set(tileManager.getServerInfo().getMaxZoomLevel());
                } else {
                    super.set(newValue);
                }
            }
        };
        getChildren().addAll(tileCanvas);
        zoom.addListener((observable, oldValue, newValue) -> requestLayout());
        center.addListener((observable, oldValue, newValue) -> requestLayout());

        tileManager.serverInfoProperty().addListener((observable, oldValue, newValue) -> requestLayout());

        // panning
        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();
        tileCanvas.setOnMousePressed(event -> {
            mouseDown.set(new Point2D(event.getX(), event.getY()));
        });
        tileCanvas.setOnMouseDragged(event -> {
            double dx = event.getX() - mouseDown.get().getX();
            double dy = event.getY() - mouseDown.get().getY();

            // shift center
            TilePoint p1 = tileManager.project(center.get(), zoom.get());
            TilePoint p2 = new TilePoint(p1.getX() - dx / tileManager.getServerInfo().getTileWidth(),
                                         p1.getY() - dy / tileManager.getServerInfo().getTileHeight(),
                                         zoom.get(),
                                         tileManager);
            center.set(p2.getCoordinate());

            mouseDown.set(new Point2D(event.getX(), event.getY()));
        });
    }

    public IntegerProperty zoomProperty() {
        return zoom;
    }

    public ObjectProperty<Coordinate> centerProperty() {
        return center;
    }

    public void addLayer(MapLayer layer) {
        Objects.requireNonNull(layer);
    }

    public void removeLayer(MapLayer layer) {
        Objects.requireNonNull(layer);
    }

    private void drawTileImage(InputStream is, double x, double y) {
        try {
            tileCanvas.getGraphicsContext2D().drawImage(new Image(is), x, y);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.error(e.toString(), e);
            }
        }
    }

    private void drawTile(Tile tile, double x, double y) {
        tile.request()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(is -> drawTileImage(is, x, y),
                    throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    private void drawTiles() {
        TilePoint tilePoint = tileManager.project(center.get(), zoom.get());
        Tile tile = tilePoint.getTile();

        double tileWidth = tileManager.getServerInfo().getTileWidth();
        double tileHeight = tileManager.getServerInfo().getTileHeight();

        // draw center tile
        double x = getWidth() / 2 - tileWidth * (tilePoint.getX() - tile.getX());
        double y = getHeight() / 2 - tileHeight * (tilePoint.getY() - tile.getY());
        drawTile(tile, x, y);

        // compute number of tiles needed to fill the screen
        int n1 = (int) Math.ceil(x / tileWidth);
        int n2 = (int) Math.ceil((getWidth() - x - tileWidth) / tileWidth);
        int m1 = (int) Math.ceil(y / tileHeight);
        int m2 = (int) Math.ceil((getHeight() - y - tileHeight) / tileHeight);

        // draw other tiles
        for (int i = -n1; i <= n2; i++) {
            for (int j = -m1; j <= m2; j++) {
                if (i != 0 || j != 0) {
                    drawTile(new Tile(tile.getX() + i, tile.getY() + j, zoom.get(), tileManager),
                             x + i * tileWidth,
                             y + j * tileHeight);
                }
            }
        }
    }

    @Override
    protected void layoutChildren() {
        // resize canvas to fit the parent region
        tileCanvas.setWidth(getWidth());
        tileCanvas.setHeight(getHeight());

        // draw tiles
        drawTiles();
    }
}
