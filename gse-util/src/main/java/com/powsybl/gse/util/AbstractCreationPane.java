/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public abstract class AbstractCreationPane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.CreationPane");

    protected final BooleanProperty uniqueName = new SimpleBooleanProperty(true);

    protected final TextField nameField = new TextField();

    protected final Label fileAlreadyExistsLabel = new Label();

    public AbstractCreationPane() {
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
    }

    protected String getLabelName() {
        return RESOURCE_BUNDLE.getString("Name");
    }

    protected TextField getTextField() {
        return nameField;
    }

    protected BooleanBinding validatedProperty() {
        return nameField.textProperty().isNotEmpty().and(uniqueName);
    }

    protected void fillCreationPane() {
        add(new Label(getLabelName()), 0, 0);
        add(getTextField(), 1, 0);
        add(fileAlreadyExistsLabel, 0, 1, 2, 1);
        fileAlreadyExistsLabel.setTextFill(Color.RED);
        Platform.runLater(nameField::requestFocus);
    }


    protected void addUniqueNameListener() {
        uniqueName.addListener((observable, oldBooleanValue, newBooleanValue) -> {
            if (newBooleanValue) {
                fileAlreadyExistsLabel.setText("");
            } else {
                fileAlreadyExistsLabel.setText(MessageFormat.format(RESOURCE_BUNDLE.getString("FileAlreadyExistsInThisFolder"),
                        nameField.getText()));
            }
        });
    }
}
