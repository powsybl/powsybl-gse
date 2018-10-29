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
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class GseAlerts {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseAlerts");

    private GseAlerts() {
    }

    public static void showDraggingError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RESOURCE_BUNDLE.getString("DragError"));
        alert.setHeaderText(RESOURCE_BUNDLE.getString("Error"));
        alert.setContentText(RESOURCE_BUNDLE.getString("FileExists"));
        alert.showAndWait();
    }

    public static void showDialogError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RESOURCE_BUNDLE.getString("Error"));
        alert.setResizable(true);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static Alert deleteNodesAlert(List<? extends TreeItem> selectedTreeItems) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(RESOURCE_BUNDLE.getString("ConfirmationDialog"));
        String headerText;
        final String fileWillBeDeleted = RESOURCE_BUNDLE.getString("FileWillBeDeleted");
        AbstractNodeBase node;
        if (selectedTreeItems.size() == 1) {
            if (selectedTreeItems.get(0).getValue() instanceof ProjectNode) {
                node = (ProjectNode) selectedTreeItems.get(0).getValue();
                headerText = String.format(fileWillBeDeleted, node.getName());
            } else if (selectedTreeItems.get(0).getValue() instanceof Node) {
                node = (Node) selectedTreeItems.get(0).getValue();
                headerText = String.format(fileWillBeDeleted, node.getName());
            } else {
                headerText = String.format(fileWillBeDeleted, "the selected file");
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
