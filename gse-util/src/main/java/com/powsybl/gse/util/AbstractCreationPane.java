/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import com.powsybl.afs.AbstractNodeBase;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public abstract class AbstractCreationPane<F> extends GridPane {

    protected final NameTextField nameTextField;

    protected AbstractNodeBase node;

    public AbstractCreationPane(F node) {
        Objects.requireNonNull(node);
        this.node = (AbstractNodeBase) node;
        nameTextField = createNameTextField();
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(getNameLabel(), 0, 0);
        add(nameTextField.getInputField(), 1, 0);
        add(nameTextField.getFileAlreadyExistsLabel(), 0, 1, 2, 1);
        Platform.runLater(nameTextField.getInputField()::requestFocus);
    }

    protected Label getNameLabel() {
        return nameTextField.getNameLabel();
    }

    protected abstract NameTextField createNameTextField();

}
