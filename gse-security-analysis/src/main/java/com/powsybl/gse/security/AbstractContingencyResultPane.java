/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

import java.util.ResourceBundle;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
abstract class AbstractContingencyResultPane extends BorderPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private static void onMouseClickedOnFilter(BorderPane borderPane, LimitViolationsFilterPane filterPane) {
        if (borderPane.getRight() != null) {
            borderPane.setRight(null);
        } else {
            borderPane.setRight(new ScrollPane(filterPane));
        }
    }

    static <S, T> TableColumn<S, T> createColumn(String type) {
        TableColumn<S, T> column = new TableColumn<>(RESOURCE_BUNDLE.getString(type));
        column.setUserData(type);
        return column;
    }

    static ToggleButton createFilterButton(BorderPane borderPane, LimitViolationsFilterPane filterPane) {
        ToggleButton filterButton = new ToggleButton();
        Label label = new Label(RESOURCE_BUNDLE.getString("Filters"));
        label.setRotate(90);
        filterButton.setGraphic(new Group(label));
        filterButton.getStyleClass().add("filter-button");
        filterButton.setOnMouseClicked(event -> onMouseClickedOnFilter(borderPane, filterPane));
        return filterButton;
    }

}
