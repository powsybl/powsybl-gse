/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.Folder;
import com.powsybl.afs.ProjectFolder;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class NewFolderPane<F> extends AbstractCreationPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NewFolderPane");

    private final Function<String, F> folderCreator;

    private NewFolderPane(Function<String, F> folderCreator, AbstractNodeBase folder) {
        super(folder);
        this.folderCreator = Objects.requireNonNull(folderCreator);
    }

    private BooleanBinding validatedProperty() {
        return nameTextField.okProperty();
    }

    private F createFolder() {
        String name = nameTextField.getText();
        return folderCreator.apply(name);
    }

    public static Optional<Folder> showAndWaitDialog(Window window, Folder parent) {
        return showAndWaitDialog(window, parent, parent::createFolder, parent);
    }

    public static Optional<ProjectFolder> showAndWaitDialog(Window window, ProjectFolder parent) {
        return showAndWaitDialog(window, parent, parent::createFolder, parent);
    }

    public static <F> Optional<F> showAndWaitDialog(Window window, F parent, Function<String, F> folderCreator,
                                                    AbstractNodeBase folder) {
        Objects.requireNonNull(window);
        Objects.requireNonNull(parent);
        Objects.requireNonNull(folderCreator);
        Objects.requireNonNull(folder);

        Dialog<F> dialog = new Dialog<>();
        try {
            dialog.setTitle(RESOURCE_BUNDLE.getString("NewFolder"));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            NewFolderPane<F> newProjectPane = new NewFolderPane<>(folderCreator, folder);
            newProjectPane.setPrefSize(350, 100);
            dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(newProjectPane.validatedProperty().not());
            dialog.getDialogPane().setContent(newProjectPane);
            dialog.setResizable(true);
            dialog.initOwner(window);
            dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? newProjectPane.createFolder() : null);
            return dialog.showAndWait();
        } finally {
            dialog.close();
        }
    }
}
