/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.AppData;
import com.powsybl.afs.Node;
import com.powsybl.gse.spi.GseContext;
import javafx.stage.Window;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeSelectionPane<T extends Node> extends AbstractNodeSelectionPane<T> {

    public NodeSelectionPane(AppData data, String label, Window window, GseContext context, Class<T> filter, Class<?>... otherFilters) {
        this(data, label, false, window, context, filter, otherFilters);
    }

    public NodeSelectionPane(AppData data, String label, boolean mandatory, Window window, GseContext context, Class<T> filter, Class<?>... otherFilters) {
        super(label, () -> NodeChooser.showAndWaitDialog(window, data, context, filter, otherFilters), mandatory, context);
    }
}

