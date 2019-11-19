/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Optional;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class FileImportManager {

    private FileChooser fileChooser = new FileChooser();

    public FileImportManager(String fileChooserPaneTitle) {
        fileChooser.setTitle(fileChooserPaneTitle);
    }

    public FileImportManager(String fileChooserPaneTitle, String fileType, String... fileExtensions) {
        fileChooser.setTitle(fileChooserPaneTitle);
        filter(fileType, fileExtensions);
    }

    public void filter(String description, final String... extensions) {
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
    }

    public void setInitialDirectory(String lastPathKey) {
        File lastPathFile = new File(lastPathKey);
        if (!lastPathKey.isEmpty() && lastPathFile.exists()) {
            fileChooser.setInitialDirectory(lastPathFile);
        }
    }

    public Optional<File> showAndWait(Window window) {
        File file = fileChooser.showOpenDialog(window);
        return Optional.ofNullable(file);
    }
}
