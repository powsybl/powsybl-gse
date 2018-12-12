/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.Folder;
import com.powsybl.afs.Project;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.NodeChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewProjectPane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NewProjectPane");

    private final TextField folderTextField = new TextField();
    private final Button folderSelectionButton = new Button("...");
    private final TextField nameTextField = new TextField();
    private final TextField descriptionTextField = new TextField();

    private final Label fileAlreadyExistsLabel = new Label();
    private final BooleanProperty uniqueName = new SimpleBooleanProperty(true);

    private final ObjectProperty<Folder> folderProperty = new SimpleObjectProperty<>();

    private final ValidationSupport validationSupport = new ValidationSupport();
    private final Validator folderValidator = Validator.createEmptyValidator(MessageFormat.format(RESOURCE_BUNDLE.getString("MandatoryParameter"),
                                                                                                  RESOURCE_BUNDLE.getString("Folder")));
    private final Validator nameValidator = Validator.createEmptyValidator(MessageFormat.format(RESOURCE_BUNDLE.getString("MandatoryParameter"),
                                                                                                RESOURCE_BUNDLE.getString("Name")));

    public NewProjectPane(Window window, AppData appData, GseContext context) {
        folderSelectionButton.setOnAction(event -> {
            Optional<Folder> folder = NodeChooser.showAndWaitDialog(window, appData, context,
                (node, treeModel) -> Folder.class.isAssignableFrom(node.getClass()) && ((Folder) node).isWritable());
            folder.ifPresent(folderProperty::setValue);
        });
        folderTextField.textProperty().bind(Bindings.createObjectBinding(() -> {
            Folder folder = folderProperty.get();
            if (folder == null) {
                return null;
            } else {
                String path = folder.getPath().toString();
                return folder.getParent().isPresent() ? path : path + AppFileSystem.PATH_SEPARATOR;
            }
        }, folderProperty));
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        folderTextField.setEditable(false);

        Predicate<String> folderUnique = name -> folderProperty.get() == null || name == null || !folderProperty.get().getChild(name).isPresent();
        nameTextField.textProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(folderUnique.test(newName)));

        uniqueName.addListener((observable, oldUnique, newUnique) -> {
            if (newUnique) {
                fileAlreadyExistsLabel.setText(null);
            } else {
                fileAlreadyExistsLabel.setText(MessageFormat.format(RESOURCE_BUNDLE.getString("ElementAlreadyExists"),
                    nameTextField.getText()));
            }
        });

        folderProperty.addListener((observable, oldName, newName) -> uniqueName.setValue(folderUnique.test(nameTextField.textProperty().get())));

        add(new Label(RESOURCE_BUNDLE.getString("Folder")), 0, 0);
        add(folderTextField, 1, 0);
        add(folderSelectionButton, 2, 0);
        add(new Label(RESOURCE_BUNDLE.getString("Name")), 0, 1);
        add(nameTextField, 1, 1, 2, 1);
        add(new Label(RESOURCE_BUNDLE.getString("Description")), 0, 2);
        add(descriptionTextField, 1, 2, 2, 1);
        add(fileAlreadyExistsLabel, 0, 3, 3, 1);
        fileAlreadyExistsLabel.setTextFill(Color.RED);

        validationSupport.registerValidator(nameTextField, nameValidator);
        Platform.runLater(() -> {
            validationSupport.registerValidator(folderTextField, folderValidator);
            nameTextField.setText(null);
        });
    }

    public BooleanBinding validatedProperty() {
        return folderProperty.isNotNull().and(nameTextField.textProperty().isNotNull()).and(nameTextField.textProperty().isNotEmpty());
    }

    public Project createProject() {
        String name = nameTextField.getText();
        String description = descriptionTextField.getText();
        Project project = folderProperty.get().createProject(name);
        project.setDescription(description);
        return project;
    }

    public static Optional<Project> showAndWaitDialog(Window window, AppData appData, GseContext context) {
        Dialog<Project> dialog = new Dialog<>();
        try {
            dialog.setTitle(RESOURCE_BUNDLE.getString("NewProject"));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            NewProjectPane newProjectPane = new NewProjectPane(window, appData, context);
            newProjectPane.setPrefSize(400, 200);
            dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(newProjectPane.validatedProperty().not());
            dialog.getDialogPane().setContent(newProjectPane);
            dialog.setResizable(false);
            dialog.initOwner(window);
            dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? newProjectPane.createProject() : null);
            return dialog.showAndWait();
        } finally {
            dialog.close();
        }
    }
}
