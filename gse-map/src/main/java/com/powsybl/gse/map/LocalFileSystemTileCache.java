package com.powsybl.gse.map;

import io.reactivex.Maybe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class LocalFileSystemTileCache implements TileCache {

    private final Path dir;

    public LocalFileSystemTileCache() {
        dir = Paths.get(System.getProperty("user.home")).resolve(".powsybl-map");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path getYFile(Tile tile, Path xDir) {
        return xDir.resolve(Integer.toString(tile.getY()) + ".png");
    }

    private Path getXDir(Tile tile) {
        return dir.resolve(Integer.toString(tile.getZoom()))
                  .resolve(Integer.toString(tile.getX()));
    }

    @Override
    public Maybe<InputStream> readTile(Tile tile) {
        Objects.requireNonNull(tile);
        return Maybe.create(maybeEmitter -> {
            Path xDir = getXDir(tile);
            Path yFile = getYFile(tile, xDir);
            if (Files.exists(yFile)) {
                maybeEmitter.onSuccess(Files.newInputStream(yFile));
            } else {
                maybeEmitter.onComplete();
            }
        });
    }

    @Override
    public OutputStream writeTile(Tile tile) {
        Objects.requireNonNull(tile);
        Path xDir = getXDir(tile);
        Path yFile = getYFile(tile, xDir);
        try {
            Files.createDirectories(xDir);
            return Files.newOutputStream(yFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
