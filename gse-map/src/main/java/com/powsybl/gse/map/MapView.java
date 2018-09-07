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

    private final TileView tileView;

    private final Canvas canvas = new Canvas();

    public MapView(TileView tileView) {
        this.tileView = Objects.requireNonNull(tileView);
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

    private void drawTile(TilePoint tilePoint, int x, int y) {
        Platform.runLater(() -> canvas.getGraphicsContext2D().fillText("Loading...",
                                                                       x + tileView.getDescriptor().getWidth() / 2,
                                                                       y + tileView.getDescriptor().getHeight() / 2));
        tilePoint.request()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(response -> drawTile(response, x, y),
                    throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    private void drawTiles(int zoom) {
        TilePoint tilePoint = tileView.project(new Coordinate(7.909167d, 47.968056d), zoom);
        drawTile(tilePoint, 0, 0);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i != 0 || j != 0) {
                    TilePoint tilePoint2 = new TilePoint((int) Math.floor(tilePoint.getX()) + i,
                                                         (int) Math.floor(tilePoint.getY()) + j,
                                                         zoom,
                            tileView);
                    drawTile(tilePoint2, i * 256, j * 256);
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
