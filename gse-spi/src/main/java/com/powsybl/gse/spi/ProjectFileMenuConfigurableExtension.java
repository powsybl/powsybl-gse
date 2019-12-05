/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.afs.ProjectFile;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

/**
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public interface ProjectFileMenuConfigurableExtension<T extends ProjectFile> {

    default Node getMenuGraphic(T file) {
        return null;
    }

    String getMenuText(T file);

    default String getMenuGroup() {
        return null;
    }

    default KeyCombination getMenuKeyCode() {
        return null;
    }

    default boolean isMenuEnabled(T file) {
        return true;
    }
}
