package com.powsybl.gse.map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapView extends Region {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapView.class);

    private final TileManager tileManager;

    private final Canvas canvas = new Canvas(500, 500);

    public MapView(TileManager tileManager) {
        this.tileManager = Objects.requireNonNull(tileManager);
        getChildren().addAll(canvas);
    }

    private void drawTile(Response response, int x, int y) {
        if (response.getStatusCode() == HttpResponseStatus.OK.code()) {
            try (InputStream is = response.getResponseBodyAsStream()) {
                canvas.getGraphicsContext2D().drawImage(new Image(is), x, y);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void drawTile(Tile tile, int x, int y) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                canvas.getGraphicsContext2D().fillText("Loading...",
                                                       x + tileManager.getDescriptor().getWidth() / 2,
                                                       y + tileManager.getDescriptor().getHeight() / 2);
            }
        });
        tile.request()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(response -> drawTile(response, x, y),
                           throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    private void drawTiles(int zoom) {
        Tile tile = tileManager.getTile(new Coordinate(7.909167d, 47.968056d), zoom);
        drawTile(tile, 0, 0);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i != 0 || j != 0) {
                    Tile tile2 = new Tile(tile.getX() + i, tile.getY() + j, zoom, tileManager);
                    drawTile(tile2, i * 256, j * 256);
                }
            }
        }
    }

    @Override
    protected void layoutChildren() {
        // resize canvas to fit the parent region
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());

        // draw tiles
        drawTiles(10);
    }
}
