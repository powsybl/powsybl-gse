/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class ContingenciesSplitPane extends SplitPane {
    private Map<String, LimitViolationsResult> limitViolationsResultPerContingency = new HashMap<>();

    private final ListView<String> contingencyListView = new ListView<>();

    private final LimitViolationsTableView limitViolationsTableView = new LimitViolationsTableView(FXCollections.observableArrayList());

    ContingenciesSplitPane() {
        setDividerPositions(0.2);
        getItems().setAll(contingencyListView, limitViolationsTableView);

        contingencyListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> refreshLimitViolationsTableView(newValue));
    }

    private void refreshLimitViolationsTableView(String contingencyId) {
        if (contingencyId != null && limitViolationsResultPerContingency.containsKey(contingencyId)) {
            limitViolationsTableView.getItems()
                    .setAll(limitViolationsResultPerContingency.get(contingencyId).getLimitViolations());
        } else {
            limitViolationsTableView.getItems().clear();
        }
    }

    void resetWithSecurityAnalysisResults(SecurityAnalysisResult results) {
        limitViolationsResultPerContingency = results.getPostContingencyResults().stream()
                .collect(Collectors.toMap(pcr -> pcr.getContingency().getId(), PostContingencyResult::getLimitViolationsResult));
        contingencyListView.getItems().setAll(results.getPostContingencyResults().stream().map(pcr -> pcr.getContingency().getId()).collect(Collectors.toList()));
    }
}
