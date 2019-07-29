/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ModificationScript;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.spi.ProjectFileCreatorExtension;
import com.powsybl.gse.util.Glyph;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileCreatorExtension.class)
public class ActionScriptCreatorExtension implements ProjectFileCreatorExtension {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ActionScriptCreator");

    @Override
    public Class<? extends ProjectFile> getProjectFileType() {
        return ModificationScript.class;
    }

    @Override
    public Node getMenuGraphic() {
        return Glyph.createAwesomeFont('\uf0e7')
                .size("1.4em")
                .color("orange");
    }

    @Override
    public String getMenuText() {
        return RESOURCE_BUNDLE.getString("CreateActionScript") + "...";
    }

    @Override
    public ProjectFileCreator newCreator(ProjectFolder folder, Scene scene, GseContext context) {
        return new ActionScriptCreator(folder);
    }
}
