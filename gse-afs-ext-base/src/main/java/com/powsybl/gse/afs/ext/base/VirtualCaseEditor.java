/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ext.base.VirtualCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileEditor;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class VirtualCaseEditor extends AbstractVirtualCasePane<VirtualCase> implements ProjectFileEditor {

    public VirtualCaseEditor(VirtualCase virtualCase, Scene scene, GseContext context) {
        super(virtualCase, scene, context);
    }

    @Override
    public void edit() {
        nameTextField.setText(node.getName());
        nameTextField.getInputField().setDisable(true);
        node.getCase().ifPresent(aCase -> caseSelectionPane.nodeProperty().setValue(aCase));
        node.getScript().ifPresent(aScript -> scriptSelectionPane.nodeProperty().setValue(aScript));
    }

    @Override
    public void saveChanges() {
        node.setCase(caseSelectionPane.nodeProperty().getValue());
        node.setScript(scriptSelectionPane.nodeProperty().getValue());
    }
}
