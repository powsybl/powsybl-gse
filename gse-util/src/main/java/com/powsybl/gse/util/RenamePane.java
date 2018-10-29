/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.Node;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import com.powsybl.afs.AbstractNodeBase;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * @author Nassirou Nambiema <nassirou.nambiema@rte-france.com>
 */
public final class RenamePane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.RenamePane");

    private final TextField nameTextField = new TextField();

    private final Label fileAlreadyExistLabel = new Label();

    private final BooleanProperty uniqueName = new SimpleBooleanProperty(true);

    public RenamePane() {

    }

    private RenamePane(AbstractNodeBase node, Predicate<String> nodeNameUnique) {
        Objects.requireNonNull(node);
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
        nameTextField.textProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(newName == null || nodeNameUnique.test(newName)));
        nameTextField.setText(node.getName());
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

    private TextField getNameTextField() {
        return nameTextField;
    }

    public Optional<String> showAndWaitDialog(ProjectNode selectedNode) {
        return showAndWaitDialog(selectedNode, name -> !selectedNode.getParent().get().getChild(name).isPresent());
    }

    public Optional<String> showAndWaitDialog(Node selectedNode) {
        return showAndWaitDialog(selectedNode, name -> !selectedNode.getParent().get().getChild(name).isPresent());
    }

    public Optional<String> showAndWaitDialog(AbstractNodeBase node, Predicate<String> nodeNameUnique) {
        Dialog dialog = new Dialog<>();
        try {
            dialog.setTitle(RESOURCE_BUNDLE.getString("RenameFile"));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            RenamePane renamePane = new RenamePane(node, nodeNameUnique);
            renamePane.setPrefSize(350, 100);
            dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(renamePane.validatedProperty().not());
            dialog.getDialogPane().setContent(renamePane);
            dialog.setResizable(true);
            dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? renamePane.getNameTextField().getText() : null);

            return dialog.showAndWait();
        } finally {
            dialog.close();
        }
    }
}

