/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.google.auto.service.AutoService;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewerExtension;
import com.powsybl.gse.util.Glyph;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileViewerExtension.class)
public class SecurityAnalysisResultViewerExtension implements ProjectFileViewerExtension<SecurityAnalysisRunner> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    @Override
    public Class<SecurityAnalysisRunner> getProjectFileType() {
        return SecurityAnalysisRunner.class;
    }

    @Override
    public Class<?> getAdditionalType() {
        return null;
    }

    @Override
    public Node getMenuGraphic(SecurityAnalysisRunner runner) {
        return Glyph.createAwesomeFont('\uf132');
    }

    @Override
    public String getMenuText(SecurityAnalysisRunner runner) {
        return RESOURCE_BUNDLE.getString("ShowResults");
    }

    @Override
    public SecurityAnalysisResultViewer newViewer(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        return new SecurityAnalysisResultViewer(runner, scene, context);
    }

    @Override
    public boolean isMenuEnabled(SecurityAnalysisRunner runner) {
        return runner.hasResult();
    }
}
