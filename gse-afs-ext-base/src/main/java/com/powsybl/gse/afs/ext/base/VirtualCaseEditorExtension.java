/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ext.base.VirtualCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileEditor;
import com.powsybl.gse.spi.ProjectFileEditorExtension;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
@AutoService(ProjectFileEditorExtension.class)
public class VirtualCaseEditorExtension implements ProjectFileEditorExtension<VirtualCase> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.VirtualCaseCreator");

    @Override
    public Class<VirtualCase> getProjectFileType() {
        return VirtualCase.class;
    }

    @Override
    public Class<?> getAdditionalType() {
        return null;
    }

    @Override
    public String getMenuGroup() {
        return RESOURCE_BUNDLE.getString("VirtualCaseName");
    }

    @Override
    public String getMenuText(VirtualCase virtualCase) {
        return RESOURCE_BUNDLE.getString("EditCalculatedCase") + "...";
    }

    @Override
    public ProjectFileEditor newEditor(VirtualCase virtualCase, Scene scene, GseContext context) {
        return new VirtualCaseEditor(virtualCase, scene, context);
    }

}
