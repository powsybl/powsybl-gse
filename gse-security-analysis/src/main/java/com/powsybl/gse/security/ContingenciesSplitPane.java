package com.powsybl.gse.security;

import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ContingenciesSplitPane extends SplitPane {
    private Map<String, LimitViolationsResult> limitViolationsResultPerContingency = new HashMap<>();

    private ListView<String> contingencyListView = new ListView<>();

    private LimitViolationsTableView limitViolationsTableView = new LimitViolationsTableView();

    ContingenciesSplitPane() {
        super();
        this.getItems().setAll(contingencyListView, limitViolationsTableView);

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
