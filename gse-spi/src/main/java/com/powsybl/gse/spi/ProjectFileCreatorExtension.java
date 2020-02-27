/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCodeCombination;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ProjectFileCreatorExtension {

    Class<? extends ProjectFile> getProjectFileType();

    default Node getMenuGraphic() {
        return null;
    }

    default String getMenuGroup() {
        return null;
    }

    default int getMenuOrder() {
        return 0;
    }

    String getMenuText();

    KeyCodeCombination getMenuKeycode();

    default String getModuleConfigName() {
        return null;
    }

    ProjectFileCreator newCreator(ProjectFolder folder, Scene scene, GseContext context);
}
