/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.afs.ProjectFile;
import javafx.scene.Scene;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ProjectFileExecutionTaskExtension<T extends ProjectFile, C> {

    Class<T> getProjectFileType();

    Class<?> getAdditionalType();

    String getMenuText();

    default boolean isMenuEnabled(T file) {
        return true;
    }

    ExecutionTaskConfigurator<C> createConfigurator(T projectFile, Scene scene, GseContext context);

    void execute(T projectFile, C config);
}
