/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.contingency.Contingency;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.PostContingencyResult;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class PostContingencyResultPane extends AbstractContingencyResultPane implements LimitViolationsResultPane {

    class ResultRow {

        protected final PostContingencyResult result;

        ResultRow(PostContingencyResult result) {
            this.result = result;
        }

        Contingency getContingency() {
            return result.getContingency();
        }
    }

    class ContingencyRow extends ResultRow {

        ContingencyRow(PostContingencyResult result) {
            super(result);
        }

        List<LimitViolation> getViolations() {
            return result.getLimitViolationsResult().getLimitViolations();
        }
    }

    class LimitViolationRow extends ResultRow {

        private final int violationIndex;

        public LimitViolationRow(PostContingencyResult result, int violationIndex) {
            super(result);
            this.violationIndex = violationIndex;
        }

        LimitViolation getViolation() {
            return result.getLimitViolationsResult().getLimitViolations().get(violationIndex);
        }
    }

    private final ObservableList<ResultRow> rows = FXCollections.observableArrayList();

    private final FilteredList<ResultRow> filteredRows = new FilteredList<>(rows);

    private final TableView<ResultRow> tableView = new TableView<>(filteredRows);

    private final DecimalColumnFactory<ResultRow, Double> decimalColumnFactory = new DecimalColumnFactory<>(0);

    private final StringColumnFactory<ResultRow, String> stringColumnFactory = new StringColumnFactory<>();

    private final ObservableList<LimitViolation> violations = FXCollections.observableArrayList();

    private final ObservableList<LimitViolation> filteredViolations = FXCollections.observableArrayList();

    private final LimitViolationsFilterPane filterPane;

    PostContingencyResultPane() {
        TableColumn<ResultRow, String> contingencyColumn = createColumn("Contingency");
        contingencyColumn.setPrefWidth(200);
        contingencyColumn.setCellFactory(stringColumnFactory);
        contingencyColumn.setCellValueFactory(callback -> callback.getValue() instanceof ContingencyRow
                                                          ? new SimpleObjectProperty<>(callback.getValue().getContingency().getId())
                                                          : null);
        TableColumn<ResultRow, String> equipmentColumn = createColumn("Equipment");
        equipmentColumn.setPrefWidth(200);
        equipmentColumn.setCellFactory(stringColumnFactory);
        equipmentColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                        ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getSubjectId())
                                                        : null);
        TableColumn<ResultRow, String> violationTypeColumn = createColumn("ViolationType");
        violationTypeColumn.setPrefWidth(150);
        violationTypeColumn.setCellFactory(stringColumnFactory);
        violationTypeColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                            ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimitType().name())
                                                            : null);
        TableColumn<ResultRow, String> violationNameColumn = createColumn("ViolationName");
        violationNameColumn.setPrefWidth(150);
        violationNameColumn.setCellFactory(stringColumnFactory);
        violationNameColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                            ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimitName())
                                                            : null);
        TableColumn<ResultRow, Double> limitColumn = createColumn("Limit");
        limitColumn.setCellFactory(decimalColumnFactory);
        limitColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                    ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimit())
                                                    : null);
        TableColumn<ResultRow, Double> valueColumn = createColumn("Value");
        valueColumn.setCellFactory(decimalColumnFactory);
        valueColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                    ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getValue())
                                                    : null);
        TableColumn<ResultRow, Double> loadColumn = createColumn("Load");
        loadColumn.setPrefWidth(150);
        loadColumn.setCellFactory(decimalColumnFactory);
        loadColumn.setCellValueFactory(callback -> {
            if (callback.getValue() instanceof LimitViolationRow) {
                LimitViolation violation = ((LimitViolationRow) callback.getValue()).getViolation();
                double load = violation.getValue() / (violation.getLimit() * violation.getLimitReduction()) * 100;
                return new SimpleObjectProperty<>(load);
            }
            return null;
        });
        tableView.getColumns().setAll(contingencyColumn,
                                      equipmentColumn,
                                      violationTypeColumn,
                                      violationNameColumn,
                                      limitColumn,
                                      valueColumn,
                                      loadColumn);

        // enable multi-selection
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // enable copy/paste
        TableUtils.installCopyPasteHandler(tableView, true);

        filterPane = new LimitViolationsFilterPane(this);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(tableView);

        setCenter(borderPane);

        ToggleButton filterButton = createFilterButton(borderPane, filterPane);
        setRight(filterButton);

    }

    @Override
    public ObservableList<TableColumnBase> getColumns() {
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
        decimalColumnFactory.setPrecision(precision);
        tableView.refresh();
    }

    private void updateViolations() {
        violations.setAll(rows.stream().flatMap(row -> {
            if (row instanceof ContingencyRow) {
                return ((ContingencyRow) row).getViolations().stream();
            }
            return Stream.empty();
        }).collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    private void updateFilteredViolations() {
        filteredViolations.setAll(filteredRows.stream()
                .filter(row -> row instanceof LimitViolationRow)
                .map(row -> ((LimitViolationRow) row).getViolation())
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    @Override
    public void setFilter(Predicate<LimitViolation> predicate) {
        Objects.requireNonNull(predicate);
        filteredRows.setPredicate(row -> {
            if (row instanceof ContingencyRow) {
                // keep the contingency row if at least one of its violation is not filtered
                for (LimitViolation violation : ((ContingencyRow) row).getViolations()) {
                    if (predicate.test(violation)) {
                        return true;
                    }
                }
                return false;
            } else {
                return predicate.test(((LimitViolationRow) row).getViolation());
            }
        });
        updateFilteredViolations();
    }

    public void setResults(List<PostContingencyResult> results) {
        if (results == null || results.isEmpty()) {
            rows.clear();
        } else {
            List<ResultRow> newRows = new ArrayList<>();
            for (PostContingencyResult result : results) {
                if (!result.getLimitViolationsResult().getLimitViolations().isEmpty()) {
                    newRows.add(new ContingencyRow(result));
                    for (int i = 0; i < result.getLimitViolationsResult().getLimitViolations().size(); i++) {
                        newRows.add(new LimitViolationRow(result, i));
                    }
                }
            }
            rows.setAll(newRows);
        }
        updateViolations();
        updateFilteredViolations();
    }

    void savePreferences() {
        Preferences.userNodeForPackage(PostContingencyResultPane.class)
                .put("postContingencyResultConfig", filterPane.toJsonConfig());
    }

    void loadPreferences() {
        filterPane.loadJsonConfig(Preferences.userNodeForPackage(PostContingencyResultPane.class)
                .get("postContingencyResultConfig", null));
    }
}
