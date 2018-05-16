/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.spi.ProjectFileViewerExtension;
import com.powsybl.gse.util.Glyph;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileViewerExtension.class)
public class NetworkExplorerExtension implements ProjectFileViewerExtension<ProjectFile> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NetworkExplorer");

    static {
        Font.loadFont(Glyph.class.getResourceAsStream("/fonts/network-explorer.ttf"), 12);
    }

    @Override
    public Class<ProjectFile> getProjectFileType() {
        return ProjectFile.class;
    }

    @Override
    public Class<?> getAdditionalType() {
        return ProjectCase.class;
    }

    @Override
    public Node getMenuGraphic(ProjectFile file) {
        return new Glyph("NetworkExplorerFont", '\ue804');
    }

    @Override
    public String getMenuText(ProjectFile file) {
        return RESOURCE_BUNDLE.getString("ExploreNetwork");
    }

    @Override
    public ProjectFileViewer newViewer(ProjectFile file, Scene scene, GseContext context) {
        return new NetworkExplorer((ProjectCase) file, context);
    }

}
