/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.Project;
import com.powsybl.afs.ext.base.AbstractScript;
import com.powsybl.afs.ext.base.ModificationScript;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.NameTextField;
import com.powsybl.gse.util.ProjectNodeSelectionPane;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ResourceBundle;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class VirtualScriptCreator extends GridPane {

    protected static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.VirtualScriptCreator");

    private final NameTextField nameTextField;

    private final ProjectNodeSelectionPane<ModificationScript> scriptSelectionPane;

    public VirtualScriptCreator(AbstractScript script, Scene scene, GseContext context) {
        Project project = script.getProject();
        nameTextField = NameTextField.create(script);
        scriptSelectionPane = new ProjectNodeSelectionPane<>(project, RESOURCE_BUNDLE.getString("ConcatenateScript"), scene.getWindow(), context, ModificationScript.class);

        setVgap(5);
        setHgap(5);
        setPrefWidth(500);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(nameTextField.getNameLabel(), 0, 0);
        add(nameTextField.getInputField(), 1, 0, 2, 1);
        add(scriptSelectionPane.getLabel(), 0, 1);
        add(scriptSelectionPane.getTextField(), 1, 1);
        add(scriptSelectionPane.getButton(), 2, 1);

        add(nameTextField.getFileAlreadyExistsLabel(), 0, 2, 3, 1);

        Platform.runLater(nameTextField.getInputField()::requestFocus);
    }

    public BooleanBinding okProperty() {
        return scriptSelectionPane.nodeProperty().isNotNull().and(nameTextField.okProperty());
    }

    public String getTitle() {
        return RESOURCE_BUNDLE.getString("CreateVirtualScript");
    }
}
