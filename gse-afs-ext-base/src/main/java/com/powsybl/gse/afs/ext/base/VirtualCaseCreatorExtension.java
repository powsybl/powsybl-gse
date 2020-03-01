/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ModificationScript;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.ext.base.VirtualCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.spi.ProjectFileCreatorExtension;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileCreatorExtension.class)
public class VirtualCaseCreatorExtension implements ProjectFileCreatorExtension {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.VirtualCaseCreator");

    @Override
    public Class<? extends ProjectFile> getProjectFileType() {
        return VirtualCase.class;
    }

    @Override
    public List<Class<?>> getDependenciesTypes() {
        return ImmutableList.of(ModificationScript.class, ProjectCase.class);
    }

    @Override
    public Node getMenuGraphic() {
        return BaseExtNodeGraphicProvider.createVirtualCaseGlyph();
    }

    @Override
    public String getMenuText() {
        return RESOURCE_BUNDLE.getString("CreateCalculatedCase") + "...";
    }

    @Override
    public int getMenuOrder() {
        return 14;
    }

    @Override
    public KeyCodeCombination getMenuKeycode() {
        return new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    }

    @Override
    public ProjectFileCreator newCreator(ProjectFolder folder, Scene scene, GseContext context) {
        return new VirtualCaseCreator(folder, scene, context);
    }

    @Override
    public ProjectFileCreator newCreatorWithParameters(ProjectFolder folder, Scene scene, GseContext context, List<ProjectFile> parameters) {
        VirtualCaseCreator virtualCaseCreator = new VirtualCaseCreator(folder, scene, context);
        for (ProjectFile value : parameters) {
            if (value instanceof ImportedCase) {
                virtualCaseCreator.setCase(value);
            } else if (value instanceof VirtualCase) {
                virtualCaseCreator.setCase(value);
            } else if (value instanceof ModificationScript) {
                virtualCaseCreator.setScript((ModificationScript) value);
            }
        }
        return virtualCaseCreator;
    }
}
