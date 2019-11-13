/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import org.junit.Test;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 */
public class PowsyblLogoTest {

    @Test
    public void testPowsyblLogo() {
        //The svg is read in the constructor
        new PowsyblLogo();
    }
}
