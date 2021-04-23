/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.spi;

import com.powsybl.afs.Project;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCodeCombination;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public interface ProjectExtension {

    String getName();

    default Node getMenuGraphic() {
        return null;
    }

    default int getMenuOrder() {
        return 0;
    }

    String getMenuText();

    default KeyCodeCombination getMenuKeycode() {
        return null;
    }

    boolean isDefaultOpened();

    ProjectFileViewer newViewer(Project project, Scene scene, GseContext context);
}
