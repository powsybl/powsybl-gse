/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.AbstractModificationScript;
import com.powsybl.afs.ext.base.VirtualCaseBuilder;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class VirtualCaseCreator extends AbstractVirtualCasePane<ProjectFolder> implements ProjectFileCreator {

    public VirtualCaseCreator(ProjectFolder folder, Scene scene, GseContext context) {
        super(folder, scene, context);
    }

    public String getTitle() {
        return RESOURCE_BUNDLE.getString("CreateCalculatedCase");
    }

    @Override
    public ProjectCreationTask createTask() {
        String name = nameTextField.getText();
        ProjectFile aCase = caseSelectionPane.nodeProperty().getValue();
        AbstractModificationScript script = scriptSelectionPane.nodeProperty().getValue();
        return new ProjectCreationTask() {
            @Override
            public String getNamePreview() {
                return name;
            }

            @Override
            public void run() {
                node.fileBuilder(VirtualCaseBuilder.class)
                        .withName(name)
                        .withCase(aCase)
                        .withScript(script)
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
