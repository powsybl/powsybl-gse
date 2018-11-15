/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.Node;
import com.powsybl.gse.spi.Savable;
import javafx.scene.control.*;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class GseAlerts {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseAlerts");
    private static final String FILEWILLBEDELETED = RESOURCE_BUNDLE.getString("FileWillBeDeleted");
    private static final String GSERROR = RESOURCE_BUNDLE.getString("Error");

    private GseAlerts() {
    }

    public static void showDraggingError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RESOURCE_BUNDLE.getString("DragError"));
        alert.setHeaderText(GSERROR);
        alert.setContentText(RESOURCE_BUNDLE.getString("FileExists"));
        alert.showAndWait();
    }

    public static void showDialogError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(GSERROR);
        alert.setResizable(true);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showDeleteProjectError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RESOURCE_BUNDLE.getString("DeleteError"));
        alert.setHeaderText(GSERROR);
        alert.setContentText(RESOURCE_BUNDLE.getString("ProjectIsOpened"));
        alert.showAndWait();
    }

    public static Optional<ButtonType> showSaveAndQuitDialog(String documentName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(MessageFormat.format(RESOURCE_BUNDLE.getString("SaveBeforeClosing"), documentName));
        alert.setContentText(RESOURCE_BUNDLE.getString("WarnSaveBeforeClosing"));
        ButtonType save = new ButtonType(RESOURCE_BUNDLE.getString("Save"), ButtonBar.ButtonData.YES);
        ButtonType dontSave = new ButtonType(RESOURCE_BUNDLE.getString("DontSave"), ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(save, dontSave, ButtonType.CANCEL);

        return alert.showAndWait();
    }

    public static boolean showSaveDialog(String documentName, Savable savable) {
        Optional<ButtonType> result = GseAlerts.showSaveAndQuitDialog(documentName);
        return result.map(type -> {
            if (type.getButtonData() == ButtonBar.ButtonData.YES) {
                savable.save();
            }

            return type != ButtonType.CANCEL;
        }).orElse(false);
    }

    public static Alert deleteNodesAlert(List<? extends TreeItem> selectedTreeItems) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(RESOURCE_BUNDLE.getString("ConfirmationDialog"));
        String headerText;
        AbstractNodeBase node;
        if (selectedTreeItems.size() == 1) {
            if (selectedTreeItems.get(0).getValue() instanceof ProjectNode) {
                node = (ProjectNode) selectedTreeItems.get(0).getValue();
                headerText = String.format(FILEWILLBEDELETED, node.getName());
            } else if (selectedTreeItems.get(0).getValue() instanceof Node) {
                node = (Node) selectedTreeItems.get(0).getValue();
                headerText = String.format(FILEWILLBEDELETED, node.getName());
            } else {
                headerText = String.format(FILEWILLBEDELETED, "the selected file");
            }
        } else if (selectedTreeItems.size() > 1) {
            String names = selectedTreeItems.stream()
                    .map(selectedTreeItem -> selectedTreeItem.getValue().toString())
                    .collect(Collectors.joining(", "));
            headerText = String.format(RESOURCE_BUNDLE.getString("FilesWillBeDeleted"), names);
        } else {
            throw new AssertionError();
        }
        alert.setHeaderText(headerText);
        alert.setContentText(RESOURCE_BUNDLE.getString("DoYouConfirm"));
        return alert;
    }
}
