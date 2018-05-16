/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileCreatorExtension;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileCreatorExtension.class)
public class SecurityAnalysisRunnerCreatorExtension implements ProjectFileCreatorExtension {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    @Override
    public Class<SecurityAnalysisRunner> getProjectFileType() {
        return SecurityAnalysisRunner.class;
    }

    @Override
    public String getMenuText() {
        return RESOURCE_BUNDLE.getString("CreateSecurityAnalysis") + "...";
    }

    @Override
    public SecurityAnalysisRunnerCreator newCreator(ProjectFolder folder, Scene scene, GseContext context) {
        return new SecurityAnalysisRunnerCreator(folder, scene, context);
    }
}
