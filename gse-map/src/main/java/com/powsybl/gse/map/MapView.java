package com.powsybl.gse.map;

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.application.Platform;
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

    private final TileSpace tileSpace;

    private final Canvas canvas = new Canvas();

    public MapView(TileSpace tileSpace) {
        this.tileSpace = Objects.requireNonNull(tileSpace);
        getChildren().addAll(canvas);
    }

    private void drawTileImage(InputStream is, int x, int y) {
        try {
            canvas.getGraphicsContext2D().drawImage(new Image(is), x, y);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.error(e.toString(), e);
            }
        }
    }

    private void drawTile(Tile tile, int x, int y) {
        tile.request()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(is -> drawTileImage(is, x, y),
                    throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    private void drawTiles(int zoom) {
        TilePoint tilePoint = tileSpace.project(new Coordinate(7.909167d, 47.968056d), zoom);
        Tile tile = tilePoint.getTile();
        drawTile(tile, 0, 0);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i != 0 || j != 0) {
                    Tile tile2 = new Tile(tile.getX() + i, tile.getY() + j, zoom, tileSpace);
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
