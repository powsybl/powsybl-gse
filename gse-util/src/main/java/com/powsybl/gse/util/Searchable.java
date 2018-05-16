/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
public interface Searchable {

    String getText();

    void deselect();

    void select(int start, int end);

}
