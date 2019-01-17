/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.ResourceBundle;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public abstract class CreationPane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.CreationPane");

    protected final TextField nameTextField = new TextField();

    protected final Label fileAlreadyExistsLabel = new Label();

    public CreationPane() {
        setVgap(5);
        setHgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(new Label(RESOURCE_BUNDLE.getString("Name")), 0, 0);
        add(nameTextField, 1, 0);
        add(fileAlreadyExistsLabel, 0, 1, 2, 1);
        fileAlreadyExistsLabel.setTextFill(Color.RED);
    }
}
