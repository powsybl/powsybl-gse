/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.powsybl.security.LimitViolation;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;

import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface LimitViolationsResultPane {

    ObservableList<TableColumn> getColumns();

    ObservableList<LimitViolation> getViolations();

    ObservableList<LimitViolation> getFilteredViolations();

    void setPrecision(int precision);

    void setFilter(Predicate<LimitViolation> predicate);
}
