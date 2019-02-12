/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.Node;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.*;
import com.powsybl.afs.AbstractNodeBase;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class RenamePane extends AbstractCreationPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.RenamePane");

    private RenamePane(AbstractNodeBase node) {
        super(node);
    }

    @Override
    protected NameTextField createNameTextField() {
        return NameTextField.createWithName(node);
    }

    private BooleanBinding validatedProperty() {
        return nameTextField.okProperty();
    }

    private TextField getNameTextField() {
        return nameTextField.getInputField();
    }

    public static Optional<String> showAndWaitDialog(ProjectNode selectedNode) {
        return showAndWaitDialogs(selectedNode);
    }

    public static Optional<String> showAndWaitDialog(Node selectedNode) {
        return showAndWaitDialogs(selectedNode);
    }

    public static Optional<String> showAndWaitDialogs(AbstractNodeBase node) {
        Dialog<String> dialog = new Dialog<>();
        try {
            dialog.setTitle(RESOURCE_BUNDLE.getString("RenameFile"));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            RenamePane renamePane = new RenamePane(node);
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

