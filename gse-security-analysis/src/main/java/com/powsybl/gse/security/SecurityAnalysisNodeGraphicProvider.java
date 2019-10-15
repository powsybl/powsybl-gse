/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import com.google.auto.service.AutoService;
import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.gse.spi.NodeGraphicProvider;
import com.powsybl.gse.util.Glyph;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import javafx.scene.Node;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(NodeGraphicProvider.class)
public class SecurityAnalysisNodeGraphicProvider implements NodeGraphicProvider {

    public static Node createSecurityAnalysisRunnerGlyph() {
        return Glyph.createAwesomeFont('\uf132')
                .size("1.4em")
                .color("dimgray");
    }

    @Override
    public Node getGraphic(Object file) {
        if (file instanceof ActionScript) {
            return Glyph.createAwesomeFont('\uf0f6')
                    .size("1.2em");
        } else if (file instanceof ContingenciesProvider) {
            return Glyph.createAwesomeFont('\uf0e7')
                    .size("1.4em")
                    .color("orange");
        } else if (file instanceof SecurityAnalysisRunner) {
            return createSecurityAnalysisRunnerGlyph();
        }
        return null;
    }
}
