/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.spi.ProjectFileCreatorExtension;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileCreatorExtension.class)
public class ImportedCaseCreatorExtension implements ProjectFileCreatorExtension {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ImportedCaseCreator");

    @Override
    public Class<? extends ProjectFile> getProjectFileType() {
        return ImportedCase.class;
    }

    @Override
    public Node getMenuGraphic() {
        return BaseExtNodeGraphicProvider.createIidmGlyph();
    }

    @Override
    public String getMenuText() {
        return RESOURCE_BUNDLE.getString("ImportCase") + "...";
    }

    @Override
    public int getMenuOrder() {
        return 10;
    }

    @Override
    public KeyCodeCombination getMenuKeycode() {
        return new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    }

    @Override
    public ProjectFileCreator newCreator(ProjectFolder folder, Scene scene, GseContext context) {
        return new ImportedCaseCreator(folder, scene, context);
    }
}
