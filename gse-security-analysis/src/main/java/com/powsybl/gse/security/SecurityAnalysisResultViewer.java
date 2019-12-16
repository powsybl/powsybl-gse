/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.BadgeCount;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.afs.security.SecurityAnalysisRunner;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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

    private final BadgeCount preContBadge = new BadgeCount(0);

    private final PreContingencyResultPane preContResultPane = new PreContingencyResultPane();

    private final Tab postContTab;

    private final BadgeCount postContBadge = new BadgeCount(0);

    private final PostContingencyResultPane postContResultPane = new PostContingencyResultPane();

    private final ProgressIndicator progressIndic = new ProgressIndicator();

    private final Service<SecurityAnalysisResult> resultLoadingService;

    public SecurityAnalysisResultViewer(SecurityAnalysisRunner runner, Scene scene, GseContext context) {
        this.runner = Objects.requireNonNull(runner);
        this.scene = Objects.requireNonNull(scene);
        this.context = Objects.requireNonNull(context);

        preContBadge.setVisible(false);
        FlowPane preContGraphic = new FlowPane(3, 3, new Label(RESOURCE_BUNDLE.getString("PreContingency")), preContBadge);
        preContGraphic.setPrefWrapLength(150);
        preContTab = new Tab("", preContResultPane);
        preContTab.setGraphic(preContGraphic);
        preContTab.setClosable(false);

        postContBadge.setVisible(false);
        FlowPane postContGraphic = new FlowPane(3, 3, new Label(RESOURCE_BUNDLE.getString("PostContingency")), postContBadge);
        postContGraphic.setPrefWrapLength(150);
        postContTab = new Tab("", postContResultPane);
        postContTab.setGraphic(postContGraphic);
        postContTab.setClosable(false);

        tabPane = new TabPane(preContTab, postContTab);

        StackPane mainPane = new StackPane(tabPane, new Group(progressIndic));
        setCenter(mainPane);

        preContResultPane.getFilteredViolations().addListener((ListChangeListener<LimitViolation>) c -> {
            preContGraphic.setPrefWrapLength(140 + String.valueOf(c.getList().size()).length() * 10);
            if (preContResultPane.getViolations().size() > 0) {
                preContBadge.setCount(c.getList().size());
                preContBadge.setVisible(true);
            } else {
                preContBadge.setVisible(false);
            }
        });
        postContResultPane.getFilteredViolations().addListener((ListChangeListener<LimitViolation>) c -> {
            postContGraphic.setPrefWrapLength(140 + String.valueOf(c.getList().size()).length() * 10);
            if (postContResultPane.getViolations().size() > 0) {
                postContBadge.setCount(c.getList().size());
                postContBadge.setVisible(true);
            } else {
                postContBadge.setVisible(false);
            }
        });

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
        progressIndic.visibleProperty().bind(resultLoadingService.runningProperty());
        tabPane.disableProperty().bind(resultLoadingService.runningProperty());
        resultLoadingService.setOnSucceeded(event -> {
            SecurityAnalysisResult result = (SecurityAnalysisResult) event.getSource().getValue();
            if (result != null) {
                preContResultPane.setResult(result.getPreContingencyResult());
                postContResultPane.setResults(result.getPostContingencyResults());
            } else {
                preContResultPane.setResult(null);
                postContResultPane.setResults(null);
            }
            preContResultPane.loadPreferences();
            postContResultPane.loadPreferences();
        });
        resultLoadingService.start();
    }

    @Override
    public void dispose() {
        preContResultPane.savePreferences();
        postContResultPane.savePreferences();
    }

    @Override
    public boolean isClosable() {
        return true;
    }
}
