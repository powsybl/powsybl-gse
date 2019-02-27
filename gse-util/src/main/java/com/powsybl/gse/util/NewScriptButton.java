/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.ext.base.ModificationScript;
import com.powsybl.afs.ext.base.ModificationScriptBuilder;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.gse.spi.GseContext;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
public final class NewScriptButton {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NewScriptButton");

    private final Button button;
    private ProjectNodeSelectionPane<ProjectFolder> folderSelectionPane;
    private NameTextField nameTextField;
    private ObjectProperty<ModificationScript> scriptProperty;

    private NewScriptButton(ProjectFolder folder, Project project, ObjectProperty<ModificationScript> nodeProperty, GseContext context) {
        Node newGlyph = Glyph.createAwesomeFont('\uf0f6').size("1.3em")
                .stack(Glyph.createAwesomeFont('\uf055').color("limegreen").size("0.8em"));
        scriptProperty = nodeProperty;
        button = new Button(null, newGlyph);
        button.setOnAction(event -> showAndWaitDialog(folder, project, button.getScene().getWindow(), context));
    }

    public static Button create(ProjectNode node, ObjectProperty<ModificationScript> nodeProperty, GseContext context) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(context);

        ProjectFolder folder = node instanceof ProjectFolder ? (ProjectFolder) node : node.getParent().orElse(node.getProject().getRootFolder());

        return new NewScriptButton(folder, node.getProject(), nodeProperty, context).button;
    }

    private Dialog<Boolean> createDialog(ProjectFolder folder, Project project, Window window, GseContext context) {
        nameTextField = NameTextField.create(folder);
        folderSelectionPane = new ProjectNodeSelectionPane<>(project, RESOURCE_BUNDLE.getString("Folder"), true,
                                                             window, context, ProjectFolder.class);
        folderSelectionPane.nodeProperty().addListener((observable, oldName, newName) -> nameTextField.changeFolder(newName));
        folderSelectionPane.nodeProperty().setValue(folder);

        GridPane pane = new GridPane();
        pane.setVgap(5);
        pane.setHgap(5);
        pane.setMinWidth(350);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        pane.getColumnConstraints().addAll(column0, column1);
        pane.add(nameTextField.getNameLabel(), 0, 0);
        pane.add(nameTextField.getInputField(), 1, 0, 2, 1);
        pane.add(folderSelectionPane.getLabel(), 0, 1);
        pane.add(folderSelectionPane.getTextField(), 1, 1, 2, 1);
        pane.add(folderSelectionPane.getButton(), 3, 1);
        pane.add(nameTextField.getFileAlreadyExistsLabel(), 0, 2, 4, 1);
        BooleanBinding okProperty = folderSelectionPane.nodeProperty().isNotNull()
                                                       .and(nameTextField.okProperty());
        Platform.runLater(nameTextField.getInputField()::requestFocus);
        return new GseDialog<>(RESOURCE_BUNDLE.getString("DialogTitle"), pane, window, okProperty.not(), buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE);
    }

    private void showAndWaitDialog(ProjectFolder folder, Project project, Window window, GseContext context) {
        Dialog<Boolean> dialog = createDialog(folder, project, window, context);
        dialog.showAndWait().filter(response -> response)
                            .ifPresent(response -> {
                                ProjectFolder targetFolder = folderSelectionPane.nodeProperty().getValue();

                                ModificationScript newScript = targetFolder.fileBuilder(ModificationScriptBuilder.class)
                                                                           .withName(nameTextField.getText())
                                                                           .withType(ScriptType.GROOVY)
                                                                           .withContent("")
                                                                           .build();
                                if (scriptProperty != null) {
                                    scriptProperty.setValue(newScript);
                                }
                            });
        dialog.close();
    }
}
