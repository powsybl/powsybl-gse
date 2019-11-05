/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.spi;

/**
 * Initialization service called at app startup to allow custom routine execution
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public interface Initializer {

    /**
     * Execute code at app initialization
     */
    void run();
}
