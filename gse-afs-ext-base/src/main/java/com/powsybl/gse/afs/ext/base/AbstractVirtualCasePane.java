/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.ext.base.AbstractModificationScript;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.NameTextField;
import com.powsybl.gse.util.NewScriptButton;
import com.powsybl.gse.util.ProjectNodeSelectionPane;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractVirtualCasePane<T extends ProjectNode> extends GridPane {

    protected static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.VirtualCaseCreator");

    protected final T node;

    protected final ProjectNodeSelectionPane<ProjectFile> caseSelectionPane;
    protected final ProjectNodeSelectionPane<AbstractModificationScript> scriptSelectionPane;
    protected final NameTextField nameTextField;

    protected AbstractVirtualCasePane(T node, Scene scene, GseContext context) {
        this.node = Objects.requireNonNull(node);
        Objects.requireNonNull(scene);
        Objects.requireNonNull(context);

        Project project = node.getProject();
        nameTextField = NameTextField.create(node);
        caseSelectionPane = new ProjectNodeSelectionPane<>(project, RESOURCE_BUNDLE.getString("Case"), true, scene.getWindow(),
                                                           context, ProjectFile.class, ProjectCase.class);
        scriptSelectionPane = new ProjectNodeSelectionPane<>(project, RESOURCE_BUNDLE.getString("ModificationScript"), true,
                                                             scene.getWindow(), context, AbstractModificationScript.class);
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(nameTextField.getNameLabel(), 0, 0);
        add(nameTextField.getInputField(), 1, 0, 2, 1);
        Platform.runLater(nameTextField.getInputField()::requestFocus);
        add(caseSelectionPane.getLabel(), 0, 1);
        add(caseSelectionPane.getTextField(), 1, 1);
        add(caseSelectionPane.getButton(), 2, 1);
        add(scriptSelectionPane.getLabel(), 0, 2);
        add(scriptSelectionPane.getTextField(), 1, 2);
        add(scriptSelectionPane.getButton(), 2, 2);
        add(NewScriptButton.create(node, scriptSelectionPane.nodeProperty(), context), 3, 2);
        add(nameTextField.getFileAlreadyExistsLabel(), 0, 3, 4, 1);
    }

    public Node getContent() {
        return this;
    }

    public BooleanBinding okProperty() {
        return caseSelectionPane.nodeProperty().isNotNull()
                .and(scriptSelectionPane.nodeProperty().isNotNull())
                .and(nameTextField.okProperty());
    }

    public void dispose() {
    }
}
