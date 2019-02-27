/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.Folder;
import com.powsybl.afs.ProjectFolder;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class NewFolderPane<F> extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NewFolderPane");

    private final Function<String, F> folderCreator;

    private final TextField nameTextField = new TextField();

    private final Label fileAlreadyExistLabel = new Label();

    private final BooleanProperty uniqueName = new SimpleBooleanProperty(true);

    private NewFolderPane(Function<String, F> folderCreator, Predicate<String> folderUnique) {
        this.folderCreator = Objects.requireNonNull(folderCreator);
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(new Label(RESOURCE_BUNDLE.getString("Name")), 0, 0);
        add(nameTextField, 1, 0);
        add(fileAlreadyExistLabel, 0, 1, 2, 1);
        fileAlreadyExistLabel.setTextFill(Color.RED);
        nameTextField.textProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(newName == null || folderUnique.test(newName)));
        uniqueName.addListener((observable, oldUnique, newUnique) -> {
            if (newUnique) {
                fileAlreadyExistLabel.setText("");
            } else {
                fileAlreadyExistLabel.setText(MessageFormat.format(RESOURCE_BUNDLE.getString("FileAlreadyExistsInThisFolder"),
                                                                   nameTextField.getText()));
            }
        });
        Platform.runLater(nameTextField::requestFocus);
    }

    private BooleanBinding validatedProperty() {
        return nameTextField.textProperty().isNotEmpty().and(uniqueName);
    }

    private F createFolder() {
        String name = nameTextField.getText();
        return folderCreator.apply(name);
    }

    public static Optional<Folder> showAndWaitDialog(Window window, Folder parent) {
        return showAndWaitDialog(window, parent,
            parent::createFolder,
            name -> !parent.getChild(name).isPresent());
    }

    public static Optional<ProjectFolder> showAndWaitDialog(Window window, ProjectFolder parent) {
        return showAndWaitDialog(window, parent,
            parent::createFolder,
            name -> !parent.getChild(name).isPresent());
    }

    public static <F> Optional<F> showAndWaitDialog(Window window, F parent, Function<String, F> folderCreator,
                                                    Predicate<String> folderUnique) {
        Objects.requireNonNull(window);
        Objects.requireNonNull(parent);
        Objects.requireNonNull(folderCreator);
        Objects.requireNonNull(folderUnique);

        Dialog<F> dialog = null;
        try {
            NewFolderPane<F> newProjectPane = new NewFolderPane<>(folderCreator, folderUnique);
            newProjectPane.setPrefSize(350, 100);
            dialog = new GseDialog<>(RESOURCE_BUNDLE.getString("NewFolder"), newProjectPane, window, newProjectPane.validatedProperty().not(), buttonType -> buttonType == ButtonType.OK ? newProjectPane.createFolder() : null);
            return dialog.showAndWait();
        } finally {
            if (dialog != null) {
                dialog.close();
            }
        }
    }
}
