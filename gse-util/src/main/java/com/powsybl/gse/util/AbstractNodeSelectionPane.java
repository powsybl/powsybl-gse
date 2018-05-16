/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.Project;
import com.powsybl.gse.spi.GseContext;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractNodeSelectionPane<T extends AbstractNodeBase> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.AbstractNodeSelectionPane");

    protected final Label label;
    protected final TextField textField = TextFields.createClearableTextField();
    protected final Button button = new Button("...");

    protected final SimpleObjectProperty<T> nodeProperty = new SimpleObjectProperty<>();

    protected AbstractNodeSelectionPane(String label, Supplier<Optional<T>> nodeChooser, boolean mandatory, GseContext context) {
        Objects.requireNonNull(label);
        this.label = new Label(label + ":");
        textField.setOnKeyPressed((KeyEvent ke) -> {
            if (ke.getCode() == KeyCode.DELETE || ke.getCode() == KeyCode.BACK_SPACE) {
                nodeProperty.setValue(null);
            }
        });
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                nodeProperty.setValue(null);
            }
        });
        textField.setOnKeyTyped(Event::consume); // not editable but still clearable
        nodeProperty.addListener((observable, oldNode, newNode) -> {
            if (newNode == null) {
                textField.setText("");
            } else {
                textField.setDisable(true);
                button.setDisable(true);
                textField.setText("...");
                GseUtil.execute(context.getExecutor(), () -> {
                    String pathStr;
                    if (!newNode.getParent().isPresent()) { // root node
                        pathStr = Project.ROOT_FOLDER_NAME;
                    } else {
                        pathStr = newNode.getPath().toString();
                    }
                    Platform.runLater(() -> {
                        textField.setText(pathStr);
                        textField.setDisable(false);
                        button.setDisable(false);
                    });
                });
            }
        });
        button.setOnAction(event ->
            nodeChooser.get().ifPresent(nodeProperty::set)
        );
        if (mandatory) {
            new ValidationSupport().registerValidator(textField, Validator.createEmptyValidator(RESOURCE_BUNDLE.getString("Mandatory")));
            textField.setText(null);
        }
    }

    public Label getLabel() {
        return label;
    }

    public TextField getTextField() {
        return textField;
    }

    public Button getButton() {
        return button;
    }

    public Pane toPane() {
        GridPane pane = new GridPane();
        pane.setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        pane.getColumnConstraints().addAll(column0, column1);
        pane.add(label, 0, 0);
        pane.add(textField, 1, 0);
        pane.add(button, 2, 0);
        return pane;
    }

    public ObjectProperty<T> nodeProperty() {
        return nodeProperty;
    }
}

