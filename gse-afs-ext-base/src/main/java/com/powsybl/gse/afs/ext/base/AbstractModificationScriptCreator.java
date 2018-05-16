/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFolder;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.util.NameTextField;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractModificationScriptCreator extends GridPane implements ProjectFileCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ModificationScript");

    private final ProjectFolder folder;

    private final NameTextField nameTextField;

    protected AbstractModificationScriptCreator(ProjectFolder folder) {
        this.folder = Objects.requireNonNull(folder);
        nameTextField = NameTextField.create(folder);
        setPrefWidth(400);
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(new Label(RESOURCE_BUNDLE.getString("ScriptName") + ":"), 0, 0);
        add(nameTextField.getInputField(), 1, 0);
        add(nameTextField.getFileAlreadyExistsLabel(), 0, 1, 2, 1);
    }

    @Override
    public String getTitle() {
        return RESOURCE_BUNDLE.getString("CreateModificationScript");
    }

    protected abstract void create(ProjectFolder folder, String name);

    @Override
    public ProjectCreationTask createTask() {
        String name = nameTextField.getText();
        return new ProjectCreationTask() {
            @Override
            public String getNamePreview() {
                return name;
            }

            @Override
            public void run() {
                create(folder, name);
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

    @Override
    public Node getContent() {
        nameTextField.setText("");
        return this;
    }

    @Override
    public BooleanBinding okProperty() {
        return nameTextField.okProperty();
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
