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
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.ResourceBundle;

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

    private final ProgressIndicator preContProgressIndic = new ProgressIndicator();

    private final Tab postContTab;

    private final TableView<LimitViolation> preContTable = new TableView<>();

    private final Service<SecurityAnalysisResult> resultLoadingService;

    public SecurityAnalysisResultViewer(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        this.runner = Objects.requireNonNull(runner);
        this.scene = Objects.requireNonNull(scene);
        this.context = Objects.requireNonNull(context);
        TableColumn<LimitViolation, String> equipmentColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Equipment"));
        equipmentColumn.setPrefWidth(200);
        equipmentColumn.setCellValueFactory(new PropertyValueFactory<>("subjectId"));
        TableColumn<LimitViolation, String> violationTypeColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationType"));
        violationTypeColumn.setPrefWidth(150);
        violationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("limitType"));
        TableColumn<LimitViolation, String> violationNameColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("ViolationName"));
        violationNameColumn.setPrefWidth(150);
        violationNameColumn.setCellValueFactory(new PropertyValueFactory<>("limitName"));
        TableColumn<LimitViolation, Float> limitColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Limit"));
        limitColumn.setCellValueFactory(new PropertyValueFactory<>("limit"));
        TableColumn<LimitViolation, Float> valueColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Value"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<LimitViolation, Float> loadColumn = new TableColumn<>(RESOURCE_BUNDLE.getString("Load"));
        loadColumn.setPrefWidth(150);
        loadColumn.setCellValueFactory(features -> {
            LimitViolation violation = features.getValue();
            float load = violation.getValue() / (violation.getLimit() * violation.getLimitReduction()) * 100;
            return new SimpleObjectProperty<>(load);
        });
        preContTable.getColumns().setAll(equipmentColumn,
                                         violationTypeColumn,
                                         violationNameColumn,
                                         limitColumn,
                                         valueColumn,
                                         loadColumn);
        preContTab = new Tab(RESOURCE_BUNDLE.getString("PreContingency"), preContTable);
        preContTab.setClosable(false);
        postContTab = new Tab(RESOURCE_BUNDLE.getString("PostContingency"));
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
                preContTable.getItems().setAll(preContingencyResult.getLimitViolations());
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
