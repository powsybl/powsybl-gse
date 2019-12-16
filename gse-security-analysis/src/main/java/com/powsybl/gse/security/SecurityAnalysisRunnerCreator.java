/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.afs.security.SecurityAnalysisRunnerBuilder;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SecurityAnalysisRunnerCreator extends AbstractSecurityAnalysisRunnerPane<ProjectFolder> implements ProjectFileCreator {

    public SecurityAnalysisRunnerCreator(ProjectFolder folder, Scene scene, GseContext context) {
        super(folder, scene, context);
    }

    @Override
    public ProjectCreationTask createTask() {
        String name = nameTextField.getText();
        ProjectFile aCase = caseSelectionPane.nodeProperty().getValue();
        ProjectFile contingencyStore = contingencyStoreSelectionPane.nodeProperty().getValue();
        return new ProjectCreationTask() {
            @Override
            public String getNamePreview() {
                return name;
            }

            @Override
            public void run() {
                node.fileBuilder(SecurityAnalysisRunnerBuilder.class)
                        .withName(name)
                        .withCase(aCase)
                        .withContingencyStore(contingencyStore)
                        .build();
            }

            @Override
            public void undo() {
                throw new AssertionError("TODO"); // TODO
            }

            @Override
            public void redo() {
                throw new AssertionError("TODO"); // TODO
            }
        };
    }
}
