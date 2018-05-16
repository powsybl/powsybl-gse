/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ProjectFileCreator {

    String getTitle();

    Node getContent();

    BooleanBinding okProperty();

    ProjectCreationTask createTask();

    void dispose();
}
