/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
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
public interface ProjectFileEditorExtension<T extends ProjectFile> extends ProjectFileMenuConfigurableExtension<T> {

    Class<T> getProjectFileType();

    Class<?> getAdditionalType();

    ProjectFileEditor newEditor(T file, Scene scene, GseContext context);
}
