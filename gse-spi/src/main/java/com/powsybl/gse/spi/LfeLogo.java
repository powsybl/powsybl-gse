/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.Collection;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfeLogo extends AbstractSvgImage {

    public LfeLogo() {
        super(LfeLogo.class.getResourceAsStream("/images/logo_lfe_powsybl.svg"), 130, 0, 232, 29);
    }

    @Override
    protected Collection<Node> extractNodes(Group svg) {
        return ((Group) svg.getChildren().get(4)).getChildren();
    }
}
