package com.powsybl.gse.map;

import io.reactivex.Maybe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public Maybe<TileImage> readImage(Tile tile) {
        Objects.requireNonNull(tile);
        return Maybe.create(maybeEmitter -> {
            Path xDir = dir.resolve(Integer.toString(tile.getZoom()))
                           .resolve(Integer.toString(tile.getX()));
            Path yFile = xDir.resolve(Integer.toString(tile.getY()));
            if (Files.exists(yFile)) {
                maybeEmitter.onSuccess(() -> {
                    try {
                        return Optional.of(Files.newInputStream(yFile));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } else {
                maybeEmitter.onComplete();
            }
        });
    }
}
