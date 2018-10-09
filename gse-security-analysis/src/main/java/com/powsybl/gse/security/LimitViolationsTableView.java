/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.security.LimitViolation;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
class LimitViolationsTableView extends TableView<LimitViolation> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private final ObservableList<LimitViolation> violations = FXCollections.observableArrayList();

    private final FilteredList<LimitViolation> filteredViolations = new FilteredList<>(violations);

    private final SortedList<LimitViolation> sortedViolations = new SortedList<>(filteredViolations);

    LimitViolationsTableView() {
        sortedViolations.comparatorProperty().bind(comparatorProperty());
        setItems(sortedViolations);

        TableColumn<LimitViolation, String> equipmentColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Equipment"));
        equipmentColumn.setPrefWidth(200);
        equipmentColumn.setCellValueFactory(new PropertyValueFactory<>("subjectId"));
        TableColumn<LimitViolation, String> violationTypeColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationType"));
        violationTypeColumn.setPrefWidth(150);
        violationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("limitType"));
        TableColumn<LimitViolation, String> violationNameColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationName"));
        violationNameColumn.setPrefWidth(150);
        violationNameColumn.setCellValueFactory(new PropertyValueFactory<>("limitName"));
        TableColumn<LimitViolation, Double> limitColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Limit"));
        limitColumn.setCellValueFactory(new PropertyValueFactory<>("limit"));
        TableColumn<LimitViolation, Double> valueColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Value"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<LimitViolation, Double> loadColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Load"));
        loadColumn.setPrefWidth(150);
        loadColumn.setCellValueFactory(features -> {
            LimitViolation violation = features.getValue();
            double load = violation.getValue() / (violation.getLimit() * violation.getLimitReduction()) * 100;
            return new SimpleObjectProperty<>(load);
        });
        getColumns().setAll(equipmentColumn,
                            violationTypeColumn,
                            violationNameColumn,
                            limitColumn,
                            valueColumn,
                            loadColumn);
    }

    public ObservableList<LimitViolation> getViolations() {
        return violations;
    }

    public void setPredicate(Predicate<LimitViolation> predicate) {
        filteredViolations.setPredicate(predicate);
    }
}
