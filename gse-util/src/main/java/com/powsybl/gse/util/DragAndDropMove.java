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
