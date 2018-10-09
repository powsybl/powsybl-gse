/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PowsyblTextLogo extends AbstractSvgImage {

    public PowsyblTextLogo() {
        super(PowsyblTextLogo.class.getResourceAsStream("/images/logo_lfe_powsybl.svg"), 130, 46, 369, 69);
    }

    @Override
    protected Collection<Node> extractNodes(Group svg) {
        return Arrays.asList(svg.getChildren().get(1),
                             svg.getChildren().get(2),
                             svg.getChildren().get(3),
                             svg.getChildren().get(5),
                             svg.getChildren().get(6),
                             svg.getChildren().get(7),
                             svg.getChildren().get(8));
    }
}
