/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.control.TreeItem;

public class DragAndDropMove {
    private Object source;
    private TreeItem sourceTreeItem;

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public TreeItem getSourceTreeItem() {
        return sourceTreeItem;
    }

    public void setSourceTreeItem(TreeItem sourceTreeItem) {
        this.sourceTreeItem = sourceTreeItem;
    }
}
