/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.spi.NodeGraphicProvider;
import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class NodeGraphics {

    private static final List<NodeGraphicProvider> GRAPHIC_PROVIDERS = new ServiceLoaderCache<>(NodeGraphicProvider.class).getServices();

    private NodeGraphics() {
    }

    private static Text createFileGraphic() {
        return Glyph.createAwesomeFont('\uf016').size("1.2em");
    }

    public static Node getGraphic(Object node) {
        Objects.requireNonNull(node);
        javafx.scene.Node graphic = null;
        for (NodeGraphicProvider graphicProvider : GRAPHIC_PROVIDERS) {
            graphic = graphicProvider.getGraphic(node);
            if (graphic != null) {
                break;
            }
        }
        return graphic == null ? createFileGraphic() : graphic;
    }
}
