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
import com.powsybl.gse.util.AbstractCreationPane;
import com.powsybl.gse.util.NameTextField;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractModificationScriptCreator extends AbstractCreationPane implements ProjectFileCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ModificationScript");

    private final ProjectFolder folder;

    protected AbstractModificationScriptCreator(ProjectFolder folder) {
        super(folder);
        this.folder = Objects.requireNonNull(folder);
        setPrefWidth(400);
    }

    @Override
    protected NameTextField createNameTextField() {
        return NameTextField.create(node);
    }

    @Override
    protected Label getNameLabel() {
        return new Label(RESOURCE_BUNDLE.getString("ScriptName"));
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
