/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.control.TreeItem;

import java.util.Collections;
import java.util.List;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class DragAndDropMove {

    private List<TreeItem> sourceTreeItem = Collections.emptyList();

    public List<TreeItem> getSourceTreeItem() {
        return sourceTreeItem;
    }

    public void setSourceTreeItem(List<TreeItem> sourceTreeItem) {
        this.sourceTreeItem = Collections.unmodifiableList(sourceTreeItem);
    }
}
