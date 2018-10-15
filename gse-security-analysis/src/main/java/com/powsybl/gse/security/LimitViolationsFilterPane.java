/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class LimitViolationsFilterPane extends GridPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private LimitViolationsResultPane resultPane;

    private final CheckListView<TableColumn<?, ?>> columnsListView;

    private CheckListView<LimitViolationType> violationTypeListView;

    private CheckListView<Country> countryListView;

    private CheckListView<Double> nominalVoltagesListView;

    private final Spinner<Integer> precision = new Spinner<>(0, 14, 1);

    private final ObjectMapper mapper = new ObjectMapper();

    LimitViolationsFilterPane(LimitViolationsResultPane resultPane) {
        this.resultPane = Objects.requireNonNull(resultPane);

        setPadding(new Insets(10, 10, 10, 10));
        setStyle("-fx-background-color: white");

        columnsListView = new CheckListView<>(resultPane.getColumns());
        columnsListView.setPrefHeight(160);
        columnsListView.setCellFactory(lv -> new CheckBoxListCell<TableColumn<?, ?>>(columnsListView::getItemBooleanProperty) {
            @Override
            public void updateItem(TableColumn<?, ?> column, boolean empty) {
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

        resultPane.setPrecision(precision.getValue());
        precision.valueProperty().addListener((observable, oldValue, newValue) -> resultPane.setPrecision(newValue));

        addControl(RESOURCE_BUNDLE.getString("Columns"), columnsListView, 0, true);
        addControl(RESOURCE_BUNDLE.getString("Precision"), precision, 12, false);

        resultPane.getViolations().addListener((ListChangeListener<LimitViolation>) c -> {
            getChildren().clear();

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
        resultPane.setFilter(violation -> {
            if (checkedViolationTypes != null && !checkedViolationTypes.contains(violation.getLimitType())) {
                return false;
            }
            SubjectInfoExtension extension = violation.getExtension(SubjectInfoExtension.class);
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

    public String toJsonConfig() {
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode columnsNode = rootNode.putArray("columns");
        for (int i : columnsListView.getCheckModel().getCheckedIndices()) {
            columnsNode.add((String) columnsListView.getCheckModel().getItem(i).getUserData());
        }
        ArrayNode violationTypesNode = rootNode.putArray("violationTypes");
        if (violationTypeListView != null) {
            for (int i : violationTypeListView.getCheckModel().getCheckedIndices()) {
                violationTypesNode.add(violationTypeListView.getCheckModel().getItem(i).name());
            }
        }
        ArrayNode countriesNode = rootNode.putArray("countries");
        if (countryListView != null) {
            for (int i : countryListView.getCheckModel().getCheckedIndices()) {
                countriesNode.add(countryListView.getCheckModel().getItem(i).name());
            }
        }
        ArrayNode nominalVoltagesNode = rootNode.putArray("nominalVoltages");
        if (nominalVoltagesListView != null) {
            for (int i : nominalVoltagesListView.getCheckModel().getCheckedIndices()) {
                nominalVoltagesNode.add(nominalVoltagesListView.getCheckModel().getItem(i));
            }
        }
        rootNode.put("precision", precision.getValue());
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void loadJsonConfig(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JsonNode rootNode = mapper.readTree(json);

            ArrayNode columnsNode = (ArrayNode) rootNode.get("columns");
            Set<String> visibleColumns = new HashSet<>();
            for (int i = 0; i < columnsNode.size(); i++) {
                visibleColumns.add(columnsNode.get(i).asText());
            }
            for (TableColumn<?, ?> column : resultPane.getColumns()) {
                if (visibleColumns.contains(column.getUserData())) {
                    columnsListView.getCheckModel().check(column);
                } else {
                    columnsListView.getCheckModel().clearCheck(column);
                }
            }

            if (violationTypeListView != null) {
                ArrayNode violationTypesNode = (ArrayNode) rootNode.get("violationTypes");
                Set<LimitViolationType> visibleViolationTypes = EnumSet.noneOf(LimitViolationType.class);
                for (int i = 0; i < violationTypesNode.size(); i++) {
                    visibleViolationTypes.add(LimitViolationType.valueOf(violationTypesNode.get(i).asText()));
                }
                for (LimitViolationType violationType : violationTypeListView.getItems()) {
                    if (visibleViolationTypes.contains(violationType)) {
                        violationTypeListView.getCheckModel().check(violationType);
                    } else {
                        violationTypeListView.getCheckModel().clearCheck(violationType);
                    }
                }
            }

            if (countryListView != null) {
                ArrayNode countriesNode = (ArrayNode) rootNode.get("countries");
                Set<Country> visibleCountries = EnumSet.noneOf(Country.class);
                for (int i = 0; i < countriesNode.size(); i++) {
                    visibleCountries.add(Country.valueOf(countriesNode.get(i).asText()));
                }
                for (Country country : countryListView.getItems()) {
                    if (visibleCountries.contains(country)) {
                        countryListView.getCheckModel().check(country);
                    } else {
                        countryListView.getCheckModel().clearCheck(country);
                    }
                }
            }

            if (nominalVoltagesListView != null) {
                ArrayNode nominalVoltagesNode = (ArrayNode) rootNode.get("nominalVoltages");
                Set<Double> visibleNominalVoltages = new HashSet<>();
                for (int i = 0; i < nominalVoltagesNode.size(); i++) {
                    visibleNominalVoltages.add(nominalVoltagesNode.get(i).asDouble());
                }
                for (Double nominalVoltage : nominalVoltagesListView.getItems()) {
                    if (visibleNominalVoltages.contains(nominalVoltage)) {
                        nominalVoltagesListView.getCheckModel().check(nominalVoltage);
                    } else {
                        nominalVoltagesListView.getCheckModel().clearCheck(nominalVoltage);
                    }
                }
            }

            precision.getValueFactory().setValue(rootNode.get("precision").asInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
