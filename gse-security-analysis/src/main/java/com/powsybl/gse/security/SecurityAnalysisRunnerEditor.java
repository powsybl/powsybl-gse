/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileEditor;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SecurityAnalysisRunnerEditor extends AbstractSecurityAnalysisRunnerPane<SecurityAnalysisRunner> implements ProjectFileEditor {

    public SecurityAnalysisRunnerEditor(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        super(runner, scene, context);
    }

    @Override
    public void edit() {
        nameTextField.setText(node.getName());
        nameTextField.setDisable(true);
        node.getCase().ifPresent(aCase -> caseSelectionPane.nodeProperty().setValue(aCase));
        node.getContingencyStore().ifPresent(contingencyStore -> contingencyStoreSelectionPane.nodeProperty().setValue(contingencyStore));
    }

    @Override
    public void saveChanges() {
        throw new AssertionError("TODO");
    }
}
