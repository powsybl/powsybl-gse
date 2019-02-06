/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.Node;
import javafx.scene.control.*;
import com.powsybl.afs.AbstractNodeBase;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class RenamePane extends AbstractCreationPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.RenamePane");

    private RenamePane(AbstractNodeBase node, Predicate<String> nodeNameUnique) {
        super();
        fillCreationPane();
        Objects.requireNonNull(node);
        nameField.textProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(newName == null || nodeNameUnique.test(newName)));
        nameField.setText(node.getName());
        addUniqueNameListener();
    }

    private TextField getNameTextField() {
        return nameField;
    }

    public static Optional<String> showAndWaitDialog(ProjectNode selectedNode) {
        return showAndWaitDialog(selectedNode, name -> !selectedNode.getParent().map(f -> f.getChild(name).isPresent()).orElse(false));
    }

    public static Optional<String> showAndWaitDialog(Node selectedNode) {
        return showAndWaitDialog(selectedNode, name -> !selectedNode.getParent().map(f -> f.getChild(name).isPresent()).orElse(false));
    }

    public static Optional<String> showAndWaitDialog(AbstractNodeBase node, Predicate<String> nodeNameUnique) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(nodeNameUnique);
        Dialog<String> dialog = new Dialog<>();
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

