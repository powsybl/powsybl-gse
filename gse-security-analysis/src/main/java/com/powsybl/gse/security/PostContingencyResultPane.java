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
import javafx.collections.transformation.SortedList;
import javafx.geometry.Side;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.HiddenSidesPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class PostContingencyResultPane extends BorderPane implements LimitViolationsResultPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

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

    private final SortedList<ResultRow> sortedRows = new SortedList<>(filteredRows);

    private final TableView<ResultRow> tableView = new TableView<>(sortedRows);

    private final DecimalColumnFactory<ResultRow, Double> columnFactory = new DecimalColumnFactory<>(0);

    private final ObservableList<LimitViolation> violations = FXCollections.observableArrayList();

    PostContingencyResultPane() {
        sortedRows.comparatorProperty().bind(tableView.comparatorProperty());

        TableColumn<ResultRow, String> contingencyColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Contingency"));
        contingencyColumn.setPrefWidth(200);
        contingencyColumn.setCellValueFactory(callback -> callback.getValue() instanceof ContingencyRow
                                                          ? new SimpleObjectProperty<>(callback.getValue().getContingency().getId())
                                                          : null);
        TableColumn<ResultRow, String> equipmentColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Equipment"));
        equipmentColumn.setPrefWidth(200);
        equipmentColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                        ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getSubjectId())
                                                        : null);
        TableColumn<ResultRow, String> violationTypeColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationType"));
        violationTypeColumn.setPrefWidth(150);
        violationTypeColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                            ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimitType().name())
                                                            : null);
        TableColumn<ResultRow, String> violationNameColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationName"));
        violationNameColumn.setPrefWidth(150);
        violationNameColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                            ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimitName())
                                                            : null);
        TableColumn<ResultRow, Double> limitColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Limit"));
        limitColumn.setCellFactory(columnFactory);
        limitColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                    ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getLimit())
                                                    : null);
        TableColumn<ResultRow, Double> valueColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Value"));
        valueColumn.setCellFactory(columnFactory);
        valueColumn.setCellValueFactory(callback -> callback.getValue() instanceof LimitViolationRow
                                                    ? new SimpleObjectProperty<>(((LimitViolationRow) callback.getValue()).getViolation().getValue())
                                                    : null);
        TableColumn<ResultRow, Double> loadColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Load"));
        loadColumn.setPrefWidth(150);
        loadColumn.setCellFactory(columnFactory);
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

        LimitViolationsFilterPane filterPane = new LimitViolationsFilterPane(this);
        HiddenSidesPane hiddenSidesPane = new HiddenSidesPane();
        hiddenSidesPane.setContent(tableView);
        hiddenSidesPane.setLeft(filterPane);

        // to prevent filter pane from disappear when clicking on a control
        filterPane.setOnMouseEntered(e -> hiddenSidesPane.setPinnedSide(Side.LEFT));
        filterPane.setOnMouseExited(e -> hiddenSidesPane.setPinnedSide(null));

        setCenter(hiddenSidesPane);
    }

    @Override
    public ObservableList<TableColumn<?, ?>> getColumns() {
        return FXCollections.observableArrayList(tableView.getColumns());
    }

    @Override
    public ObservableList<LimitViolation> getViolations() {
        return violations;
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
            violations.setAll(newRows.stream().flatMap(row -> {
                if (row instanceof ContingencyRow) {
                    return ((ContingencyRow) row).getViolations().stream();
                }
                return Stream.empty();
            }).collect(Collectors.toCollection(FXCollections::observableArrayList)));
        }
    }
}
