/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.Project;
import com.powsybl.afs.ext.base.AbstractScript;
import com.powsybl.afs.ext.base.GenericScript;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.ProjectNodeSelectionPane;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class VirtualScriptCreator extends GridPane {

    protected static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.VirtualScriptCreator");

    private final AbstractScript script;

    private final ProjectNodeSelectionPane<AbstractScript> scriptSelectionPane;

    public VirtualScriptCreator(AbstractScript script, Scene scene, GseContext context) {
        this.script = Objects.requireNonNull(script);
        Project project = script.getProject();
        scriptSelectionPane = new ProjectNodeSelectionPane<>(project, RESOURCE_BUNDLE.getString("ConcatenateScript"), false, scene.getWindow(), context, (projectNode, treeModel) ->
                !projectNode.getId().equals(script.getId()) && (script.getClass().isAssignableFrom(projectNode.getClass()) || GenericScript.class.isAssignableFrom(projectNode.getClass())));

        setVgap(5);
        setHgap(5);
        setPrefWidth(500);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(scriptSelectionPane.getLabel(), 0, 0);
        add(scriptSelectionPane.getTextField(), 1, 0);
        add(scriptSelectionPane.getButton(), 2, 0);
    }

    public void create() {
        AbstractScript abstractScript = scriptSelectionPane.nodeProperty().getValue();
        if (abstractScript instanceof GenericScript) {
            script.addGenericScript((GenericScript) abstractScript);
        } else {
            script.addScript(abstractScript);
        }
    }

    public BooleanBinding okProperty() {
        return scriptSelectionPane.nodeProperty().isNotNull();
    }

    public String getTitle() {
        return RESOURCE_BUNDLE.getString("CreateVirtualScript");
    }
}
