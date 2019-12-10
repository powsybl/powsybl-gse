/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.google.auto.service.AutoService;
import com.powsybl.gse.spi.ExecutionTaskConfigurator;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileExecutionTaskExtension;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.scene.Scene;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileExecutionTaskExtension.class)
public class SecurityAnalysisExecutionTaskExtension implements ProjectFileExecutionTaskExtension<SecurityAnalysisRunner, Void> {

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
    public String getMenuText(SecurityAnalysisRunner runner) {
        return RESOURCE_BUNDLE.getString("RunSecurityAnalysis");
    }

    @Override
    public void execute(SecurityAnalysisRunner runner, Void config) {
        runner.run();
    }

    @Override
    public void clearResults(SecurityAnalysisRunner projectFile) {
        // TODO(pbuiquang)
        //projectFile.clearResults();
    }

    @Override
    public ExecutionTaskConfigurator<Void> createConfigurator(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        return null;
    }
}
