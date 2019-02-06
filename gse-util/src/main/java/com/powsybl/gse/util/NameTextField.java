/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.*;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
public final class NameTextField {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NameTextField");

    private final TextField nameTextField = new TextField();
    private final Label nameLabel = new Label(RESOURCE_BUNDLE.getString("Name") + ":");
    private final Label fileAlreadyExistsLabel = new Label();
    private final BooleanProperty uniqueName = new SimpleBooleanProperty(true);
    private Predicate<String> folderUnique;

    private NameTextField() {
        new ValidationSupport().registerValidator(nameTextField, Validator.createEmptyValidator(RESOURCE_BUNDLE.getString("MandatoryName")));
        nameTextField.setText(null);
        fileAlreadyExistsLabel.setTextFill(Color.RED);
        nameTextField.textProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(folderUnique.test(newName)));
        nameTextField.disabledProperty().addListener((observable, oldName, newName) -> uniqueName.setValue(newName));
        uniqueName.addListener((observable, oldUnique, newUnique) -> {
            if (newUnique || nameTextField.isDisabled()) {
                fileAlreadyExistsLabel.setText(null);
            } else {
                fileAlreadyExistsLabel.setText(MessageFormat.format(RESOURCE_BUNDLE.getString("FileAlreadyExistsInThisFolder"),
                        nameTextField.getText()));
            }
        });
    }

    private NameTextField(ProjectFolder folder) {
        this();
        Objects.requireNonNull(folder);
        folderUnique = name -> name == null || !folder.getChild(name).isPresent();
    }

    private NameTextField(Folder folder) {
        this();
        Objects.requireNonNull(folder);
        folderUnique = name -> name == null || !folder.getChild(name).isPresent();
    }

    public static NameTextField create(ProjectNode node) {
        Objects.requireNonNull(node);
        if (node instanceof ProjectFolder) {
            return new NameTextField((ProjectFolder) node);
        } else {
            // project file
            return node.getParent().map(NameTextField::new).orElseThrow(AssertionError::new);
        }
    }

    public static NameTextField create(Node node) {
        Objects.requireNonNull(node);
        if (node instanceof Folder) {
            return new NameTextField((Folder) node);
        } else {
            // file
            return node.getParent().map(NameTextField::new).orElseThrow(AssertionError::new);
        }
    }

    public String getText() {
        return nameTextField.getText();
    }

    public void setText(String txt) {
        nameTextField.setText(txt);
    }

    public void changeFolder(ProjectFolder folder) {
        folderUnique = name -> folder == null || name == null || !folder.getChild(name).isPresent();
        String currentValue = nameTextField.getText();
        if (currentValue != null) { // Generate a change to update uniqueName property
            nameTextField.setText("");
            nameTextField.setText(currentValue);
        }
    }

    public BooleanBinding okProperty() {
        return nameTextField.textProperty().isNotEmpty().and(uniqueName);
    }

    public TextField getInputField() {
        return nameTextField;
    }

    public Label getNameLabel() {
        return nameLabel;
    }

    public Label getFileAlreadyExistsLabel() {
        return fileAlreadyExistsLabel;
    }

}
