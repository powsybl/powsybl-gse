/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.powsybl.security.afs.SubjectInfoExtension;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.util.Callback;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.HiddenSidesPane;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SecurityAnalysisResultViewer extends BorderPane implements ProjectFileViewer {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    private final SecurityAnalysisRunner runner;

    private final Scene scene;

    private final GseContext context;

    private final TabPane tabPane;

    private final Tab preContTab;

    private final BorderPane preContPane;

    private final ProgressIndicator preContProgressIndic = new ProgressIndicator();

    private final Tab postContTab;

    private final ObservableList<LimitViolation> preContViolations = FXCollections.observableArrayList();

    private final FilteredList<LimitViolation> filteredPreContViolations = new FilteredList<>(preContViolations);

    private final SortedList<LimitViolation> sortedPreContViolations = new SortedList<>(filteredPreContViolations);

    private final TableView<LimitViolation> preContTable = new LimitViolationsTableView(sortedPreContViolations);

    private final ContingenciesSplitPane postContSplitPane = new ContingenciesSplitPane();

    private final Service<SecurityAnalysisResult> resultLoadingService;

    private static class FilterPane extends GridPane {

        public FilterPane(TableView<LimitViolation> tableView) {
            tableView.getItems().addListener((ListChangeListener<LimitViolation>) c -> {
                getChildren().clear();

                setPadding(new Insets(10, 10, 10, 10));
                setStyle("-fx-background-color: white");

                CheckListView<TableColumn<LimitViolation, ?>> columnsListView = new CheckListView<>(tableView.getColumns());
                columnsListView.setPrefHeight(150);
                columnsListView.setCellFactory(lv -> new CheckBoxListCell<TableColumn<LimitViolation, ?>>(columnsListView::getItemBooleanProperty) {
                    @Override
                    public void updateItem(TableColumn<LimitViolation, ?> column, boolean empty) {
                        super.updateItem(column, empty);
                        setText(column == null ? "" : column.getText());
                    }
                });
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
                add(new Label(RESOURCE_BUNDLE.getString("Columns") + ":"), 0, 0);
                add(columnsListView, 0, 1);
                GridPane.setMargin(columnsListView, new Insets(0, 0, 10, 0));

                int rowIndex = 2;

                Set<Country> countries = new TreeSet<>();
                Set<Double> nominalVoltages = new TreeSet<>();
                for (LimitViolation violation : c.getList()) {
                    SubjectInfoExtension extension = violation.getExtension(SubjectInfoExtension.class);
                    if (extension != null) {
                        countries.addAll(extension.getCountries());
                        nominalVoltages.addAll(extension.getNominalVoltages());
                    }
                }
                if (!countries.isEmpty()) {
                    CheckListView<Country> countryListView = new CheckListView<>();
                    countryListView.setPrefHeight(100);
                    countryListView.getItems().addAll(countries);
                    add(new Label(RESOURCE_BUNDLE.getString("Countries") + ":"), 0, rowIndex);
                    add(countryListView, 0, rowIndex + 1);
                    rowIndex += 2;
                    GridPane.setMargin(countryListView, new Insets(0, 0, 10, 0));
                }
                if (!nominalVoltages.isEmpty()) {
                    CheckListView<Double> nominalVoltagesListView = new CheckListView<>();
                    nominalVoltagesListView.setPrefHeight(100);
                    nominalVoltagesListView.getItems().addAll(nominalVoltages);
                    add(new Label(RESOURCE_BUNDLE.getString("NominalVoltages") + ":"), 0, rowIndex);
                    add(nominalVoltagesListView, 0, rowIndex + 1);
                    GridPane.setMargin(nominalVoltagesListView, new Insets(0, 0, 10, 0));
                }
            });
        }
    }

    public SecurityAnalysisResultViewer(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        this.runner = Objects.requireNonNull(runner);
        this.scene = Objects.requireNonNull(scene);
        this.context = Objects.requireNonNull(context);

        sortedPreContViolations.comparatorProperty().bind(preContTable.comparatorProperty());

        FilterPane filters = new FilterPane(preContTable);
        HiddenSidesPane preContTableAndFiltersPane = new HiddenSidesPane();
        preContTableAndFiltersPane.setContent(preContTable);
        preContTableAndFiltersPane.setLeft(filters);
        // to prevent filter pane from disappear when clicking on inside controls
        filters.setOnMouseEntered(e -> preContTableAndFiltersPane.setPinnedSide(Side.LEFT));
        filters.setOnMouseExited(e -> preContTableAndFiltersPane.setPinnedSide(null));

        preContPane = new BorderPane();
        preContPane.setCenter(preContTableAndFiltersPane);
        preContTab = new Tab(RESOURCE_BUNDLE.getString("PreContingency"), preContPane);
        preContTab.setClosable(false);

        postContTab = new Tab(RESOURCE_BUNDLE.getString("PostContingency"), postContSplitPane);
        postContTab.setClosable(false);
        tabPane = new TabPane(preContTab, postContTab);
        StackPane mainPane = new StackPane(tabPane, new Group(preContProgressIndic));
        setCenter(mainPane);

        resultLoadingService = GseUtil.createService(new Task<SecurityAnalysisResult>() {
            @Override
            protected SecurityAnalysisResult call() {
                return runner.readResult();
            }
        }, context.getExecutor());
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public void view() {
        preContProgressIndic.visibleProperty().bind(resultLoadingService.runningProperty());
        tabPane.disableProperty().bind(resultLoadingService.runningProperty());
        resultLoadingService.setOnSucceeded(event -> {
            SecurityAnalysisResult result = (SecurityAnalysisResult) event.getSource().getValue();
            if (result != null) {
                LimitViolationsResult preContingencyResult = result.getPreContingencyResult();
                preContViolations.setAll(preContingencyResult.getLimitViolations());
                postContSplitPane.resetWithSecurityAnalysisResults(result);
            } else {
                // TODO
            }
        });
        resultLoadingService.start();
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
