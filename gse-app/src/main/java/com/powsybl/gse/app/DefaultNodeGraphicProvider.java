/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.google.auto.service.AutoService;
import com.powsybl.afs.*;
import com.powsybl.gse.spi.NodeGraphicProvider;
import com.powsybl.gse.util.Glyph;
import javafx.scene.Node;
import javafx.scene.text.Text;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(NodeGraphicProvider.class)
public class DefaultNodeGraphicProvider implements NodeGraphicProvider {

    private static Text createFolder() {
        return Glyph.createAwesomeFont('\uf07b')
                .size("1.3em")
                .color("#FFDB69");
    }

    private static Text createDatabase() {
        return Glyph.createAwesomeFont('\uf1c0')
                .size("1.3em")
                .color("grey");
    }

    private static Text createProject() {
        return Glyph.createAwesomeFont('\uf07b')
                .size("1.3em")
                .color("deepskyblue");
    }

    private static Text createUnknown() {
        return Glyph.createAwesomeFont('\uf128')
                .size("1.3em")
                .color("red");
    }

    @Override
    public Node getGraphic(Object node) {
        if (node instanceof Folder) {
            if (((Folder) node).getParent().isPresent()) {
                return createFolder();
            } else {
                return createDatabase();
            }
        } else if (node instanceof ProjectFolder) {
            if (((ProjectFolder) node).getParent().isPresent()) {
                return createFolder();
            } else {
                return createProject();
            }
        } else if (node instanceof Project) {
            return createProject();
        } else if (node instanceof UnknownFile || node instanceof UnknownProjectFile) {
            return createUnknown();
        }
        return null;
    }
}
