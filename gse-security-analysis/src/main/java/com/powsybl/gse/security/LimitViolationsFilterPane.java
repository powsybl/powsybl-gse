/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.google.common.collect.Sets;
import com.powsybl.iidm.network.Country;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.afs.SubjectInfoExtension;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.CheckListView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class LimitViolationsFilterPane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private LimitViolationsTableView tableView;

    private CheckListView<LimitViolationType> violationTypeListView;

    private CheckListView<Country> countryListView;

    private CheckListView<Double> nominalVoltagesListView;

    private final Spinner<Integer> precision = new Spinner<>(0, 14, 1);

    LimitViolationsFilterPane(LimitViolationsTableView tableView) {
        this.tableView = Objects.requireNonNull(tableView);

        CheckListView<TableColumn<LimitViolation, ?>> columnsListView = new CheckListView<>(tableView.getColumns());
        columnsListView.setPrefHeight(160);
        columnsListView.setCellFactory(lv -> new CheckBoxListCell<TableColumn<LimitViolation, ?>>(columnsListView::getItemBooleanProperty) {
            @Override
            public void updateItem(TableColumn<LimitViolation, ?> column, boolean empty) {
                super.updateItem(column, empty);
                setText(column == null ? "" : column.getText());
            }
        });
        addContextMenu(columnsListView);
        columnsListView.getCheckModel().checkAll();
        columnsListView.getCheckModel().getCheckedIndices().addListener((ListChangeListener<Integer>) c1 -> {
            while (c1.next()) {
                if (c1.wasAdded()) {
                    for (int i : c1.getAddedSubList()) {
                        columnsListView.getItems().get(i).setVisible(true);
                    }
                }
                if (c1.wasRemoved()) {
                    for (int i : c1.getRemoved()) {
                        columnsListView.getItems().get(i).setVisible(false);
                    }
                }
            }
        });

        tableView.setPrecision(precision.getValue());
        precision.valueProperty().addListener((observable, oldValue, newValue) -> tableView.setPrecision(newValue));

        tableView.getViolations().addListener((ListChangeListener<LimitViolation>) c -> {
            getChildren().clear();

            setPadding(new Insets(10, 10, 10, 10));
            setStyle("-fx-background-color: white");

            addControl(RESOURCE_BUNDLE.getString("Columns"), columnsListView, 0, true);

            Set<Country> countries = new TreeSet<>();
            Set<Double> nominalVoltages = new TreeSet<>();
            for (LimitViolation violation : c.getList()) {
                SubjectInfoExtension extension = violation.getExtension(SubjectInfoExtension.class);
                if (extension != null) {
                    countries.addAll(extension.getCountries());
                    nominalVoltages.addAll(extension.getNominalVoltages());
                }
            }

            violationTypeListView = createListView(Sets.newLinkedHashSet(Arrays.asList(LimitViolationType.values())), "ViolationType", 3, 180);
            countryListView = createListView(countries, "Countries", 6, 100);
            nominalVoltagesListView = createListView(nominalVoltages, "NominalVoltages", 9, 100);

            addControl(RESOURCE_BUNDLE.getString("Precision"), precision, 12, false);
        });
    }

    private static Label createTitle(String text) {
        Label label = new Label(text + ":");
        label.setStyle("-fx-font-weight: bold");
        return label;
    }

    private void addControl(String title, Control control, int rowIndex, boolean addSeparator) {
        add(createTitle(title), 0, rowIndex);
        add(control, 0, rowIndex + 1);
        GridPane.setMargin(control, new Insets(5, 0, 10, 0));
        if (addSeparator) {
            Separator separator = new Separator();
            add(separator, 0, rowIndex + 2);
            GridPane.setMargin(separator, new Insets(0, 0, 5, 0));
        }
    }

    private static <T> void addContextMenu(CheckListView<T> listView) {
        listView.setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem selectAll = new MenuItem(RESOURCE_BUNDLE.getString("SelectAll"));
            selectAll.disableProperty().bind(Bindings.equal(listView.getItems().size(), Bindings.size(listView.getCheckModel().getCheckedIndices())));
            selectAll.setOnAction(event2 -> listView.getCheckModel().checkAll());
            MenuItem deselectAll = new MenuItem(RESOURCE_BUNDLE.getString("DeselectAll"));
            deselectAll.disableProperty().bind(Bindings.isEmpty(listView.getCheckModel().getCheckedIndices()));
            deselectAll.setOnAction(event2 -> listView.getCheckModel().clearChecks());
            contextMenu.getItems().addAll(selectAll, deselectAll);
            contextMenu.show(listView, event.getScreenX(), event.getScreenY());
        });
    }

    private <T> CheckListView<T> createListView(Set<T> values, String text, int rowIndex, double prefHeight) {
        if (values.isEmpty()) {
            return null;
        } else {
            CheckListView<T> listView = new CheckListView<>();
            addContextMenu(listView);
            listView.setPrefHeight(prefHeight);
            listView.getItems().addAll(values);
            listView.getCheckModel().checkAll();
            listView.getCheckModel().getCheckedIndices().addListener((ListChangeListener<Integer>) l -> updateFilter());
            addControl(RESOURCE_BUNDLE.getString(text), listView, rowIndex, true);
            return listView;
        }
    }

    private static <T> Set<T> getCheckedValues(CheckListView<T> listView) {
        if (listView == null) {
            return null;
        }
        return listView.getCheckModel().getCheckedIndices().stream()
                .map(i -> listView.getCheckModel().getItem(i))
                .collect(Collectors.toSet());
    }

    private void updateFilter() {
        Set<LimitViolationType> checkedViolationTypes = getCheckedValues(violationTypeListView);
        Set<Country> checkedCountries = getCheckedValues(countryListView);
        Set<Double> checkedNominaVoltages = getCheckedValues(nominalVoltagesListView);

        tableView.setPredicate(limitViolation -> {
            if (checkedViolationTypes != null && !checkedViolationTypes.contains(limitViolation.getLimitType())) {
                return false;
            }
            SubjectInfoExtension extension = limitViolation.getExtension(SubjectInfoExtension.class);
            // if no extension, we cannot filter
            if (extension == null) {
                return true;
            }
            if (checkedCountries != null && checkedCountries.stream().noneMatch(extension.getCountries()::contains)) {
                return false;
            }
            if (checkedNominaVoltages != null && checkedNominaVoltages.stream().noneMatch(extension.getNominalVoltages()::contains)) {
                return false;
            }
            return true;
        });
    }
}
