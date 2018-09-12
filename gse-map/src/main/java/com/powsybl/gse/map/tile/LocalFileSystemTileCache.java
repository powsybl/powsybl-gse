/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static Path getYFile(Tile tile, Path xDir) {
        return xDir.resolve(Integer.toString(tile.getY()) + ".png");
    }

    private Path getXDir(Tile tile) {
        return dir.resolve(tile.getServerName())
                  .resolve(Integer.toString(tile.getZoom()))
                  .resolve(Integer.toString(tile.getX()));
    }

    @Override
    public Optional<InputStream> readTile(Tile tile) {
        Objects.requireNonNull(tile);
        Path xDir = getXDir(tile);
        Path yFile = getYFile(tile, xDir);
        if (Files.exists(yFile)) {
            try {
                return Optional.of(Files.newInputStream(yFile));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Optional.empty();
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
