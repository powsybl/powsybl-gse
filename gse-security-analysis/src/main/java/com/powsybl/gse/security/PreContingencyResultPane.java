/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Side;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.HiddenSidesPane;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class PreContingencyResultPane extends BorderPane implements LimitViolationsResultPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private final ObservableList<LimitViolation> violations = FXCollections.observableArrayList();

    private final FilteredList<LimitViolation> filteredViolations = new FilteredList<>(violations);

    private final SortedList<LimitViolation> sortedViolations = new SortedList<>(filteredViolations);

    private final TableView<LimitViolation> tableView = new TableView<>(sortedViolations);

    private final DecimalColumnFactory<LimitViolation, Double> columnFactory = new DecimalColumnFactory<>(0);

    private final LimitViolationsFilterPane filterPane;

    PreContingencyResultPane() {
        sortedViolations.comparatorProperty().bind(tableView.comparatorProperty());

        TableColumn<LimitViolation, String> equipmentColumn = createColumn("Equipment");
        equipmentColumn.setPrefWidth(200);
        equipmentColumn.setCellValueFactory(callback -> new SimpleObjectProperty<>(callback.getValue().getSubjectId()));
        TableColumn<LimitViolation, String> violationTypeColumn = createColumn("ViolationType");
        violationTypeColumn.setPrefWidth(150);
        violationTypeColumn.setCellValueFactory(callback -> new SimpleObjectProperty<>(callback.getValue().getLimitType().name()));
        TableColumn<LimitViolation, String> violationNameColumn = createColumn("ViolationName");
        violationNameColumn.setPrefWidth(150);
        violationNameColumn.setCellValueFactory(callback -> new SimpleObjectProperty<>(callback.getValue().getLimitName()));
        TableColumn<LimitViolation, Double> limitColumn = createColumn("Limit");
        limitColumn.setCellFactory(columnFactory);
        limitColumn.setCellValueFactory(callback -> new SimpleObjectProperty<>(callback.getValue().getLimit()));
        TableColumn<LimitViolation, Double> valueColumn = createColumn("Value");
        valueColumn.setCellFactory(columnFactory);
        valueColumn.setCellValueFactory(callback -> new SimpleObjectProperty<>(callback.getValue().getValue()));
        TableColumn<LimitViolation, Double> loadColumn = createColumn("Load");
        loadColumn.setPrefWidth(150);
        loadColumn.setCellFactory(columnFactory);
        loadColumn.setCellValueFactory(callback -> {
            LimitViolation violation = callback.getValue();
            double load = violation.getValue() / (violation.getLimit() * violation.getLimitReduction()) * 100;
            return new SimpleObjectProperty<>(load);
        });
        tableView.getColumns().setAll(equipmentColumn,
                                      violationTypeColumn,
                                      violationNameColumn,
                                      limitColumn,
                                      valueColumn,
                                      loadColumn);

        filterPane = new LimitViolationsFilterPane(this);
        HiddenSidesPane hiddenSidesPane = new HiddenSidesPane();
        hiddenSidesPane.setContent(tableView);
        hiddenSidesPane.setLeft(filterPane);

        // to prevent filter pane from disappear when clicking on a control
        filterPane.setOnMouseEntered(e -> hiddenSidesPane.setPinnedSide(Side.LEFT));
        filterPane.setOnMouseExited(e -> hiddenSidesPane.setPinnedSide(null));

        setCenter(hiddenSidesPane);
    }

    private static <S, T> TableColumn<S, T> createColumn(String type) {
        TableColumn<S, T> column = new TableColumn<>(RESOURCE_BUNDLE.getString(type));
        column.setUserData(type);
        return column;
    }

    @Override
    public ObservableList<TableColumn> getColumns() {
        return FXCollections.observableArrayList(tableView.getColumns());
    }

    @Override
    public ObservableList<LimitViolation> getViolations() {
        return violations;
    }

    @Override
    public ObservableList<LimitViolation> getFilteredViolations() {
        return filteredViolations;
    }

    @Override
    public void setPrecision(int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("Bad precision: " + precision);
        }
        columnFactory.setPrecision(precision);
        tableView.refresh();
    }

    @Override
    public void setFilter(Predicate<LimitViolation> predicate) {
        Objects.requireNonNull(predicate);
        filteredViolations.setPredicate(predicate);
    }

    public void setResult(LimitViolationsResult result) {
        if (result == null) {
            violations.clear();
        } else {
            violations.addAll(result.getLimitViolations());
        }
    }

    void savePreferences() {
        Preferences.userNodeForPackage(PreContingencyResultPane.class)
                .put("preContingencyResultConfig", filterPane.toJsonConfig());
    }

    void loadPreferences() {
        filterPane.loadJsonConfig(Preferences.userNodeForPackage(PreContingencyResultPane.class)
                .get("preContingencyResultConfig", null));
    }
}
