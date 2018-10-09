/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.security.LimitViolation;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.HiddenSidesPane;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class PreContingencyResultPane extends BorderPane {

    private final LimitViolationsTableView tableView = new LimitViolationsTableView();

    PreContingencyResultPane() {
        LimitViolationsFilterPane filterPane = new LimitViolationsFilterPane(tableView);
        HiddenSidesPane preContTableAndFilterPane = new HiddenSidesPane();
        preContTableAndFilterPane.setContent(tableView);
        preContTableAndFilterPane.setLeft(filterPane);

        // to prevent filter pane from disappear when clicking on a control
        filterPane.setOnMouseEntered(e -> preContTableAndFilterPane.setPinnedSide(Side.LEFT));
        filterPane.setOnMouseExited(e -> preContTableAndFilterPane.setPinnedSide(null));

        setCenter(preContTableAndFilterPane);
    }

    public ObservableList<LimitViolation> getViolations() {
        return tableView.getViolations();
    }
}
