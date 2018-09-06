/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.contingency.afs.ContingencyStore;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.spi.ProjectFileViewerExtension;
import com.powsybl.gse.util.Glyph;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileViewerExtension.class)
public class ContingencyStoreEditorExtension implements ProjectFileViewerExtension<ContingencyStore> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ContingencyStore");

    @Override
    public Class<ContingencyStore> getProjectFileType() {
        return ContingencyStore.class;
    }

    @Override
    public Class<?> getAdditionalType() {
        return null;
    }

    @Override
    public Node getMenuGraphic(ContingencyStore store) {
        return Glyph.createAwesomeFont('\uf0e7');
    }

    @Override
    public String getMenuText(ContingencyStore store) {
        return RESOURCE_BUNDLE.getString("EditContingencies");
    }

    @Override
    public boolean isMenuEnabled(ContingencyStore store) {
        return true;
    }

    @Override
    public ProjectFileViewer newViewer(ContingencyStore store, Scene scene, GseContext context) {
        return new ContingencyStoreEditor(store);
    }

}
