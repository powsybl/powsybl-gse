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
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.HiddenSidesPane;

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

    private final BorderPane preContPane;

    private final ProgressIndicator preContProgressIndic = new ProgressIndicator();

    private final Tab postContTab;

    private final LimitViolationsTableView preContTable = new LimitViolationsTableView();

    private final ContingenciesSplitPane postContSplitPane = new ContingenciesSplitPane();

    private final Service<SecurityAnalysisResult> resultLoadingService;

    public SecurityAnalysisResultViewer(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        this.runner = Objects.requireNonNull(runner);
        this.scene = Objects.requireNonNull(scene);
        this.context = Objects.requireNonNull(context);

        LimitViolationsFilterPane filterPane = new LimitViolationsFilterPane(preContTable);
        HiddenSidesPane preContTableAndFilterPane = new HiddenSidesPane();
        preContTableAndFilterPane.setContent(preContTable);
        preContTableAndFilterPane.setLeft(filterPane);
        // to prevent filter pane from disappear when clicking on inside controls
        filterPane.setOnMouseEntered(e -> preContTableAndFilterPane.setPinnedSide(Side.LEFT));
        filterPane.setOnMouseExited(e -> preContTableAndFilterPane.setPinnedSide(null));

        preContPane = new BorderPane();
        preContPane.setCenter(preContTableAndFilterPane);
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
                preContTable.getViolations().setAll(preContingencyResult.getLimitViolations());
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
