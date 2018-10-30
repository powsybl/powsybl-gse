/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;

/**
 * @author Nassirou Nambiema <nassirou.nambiena@rte-france.com>
 */
public class DragAndDropMove {

    private boolean nameFound;

    private boolean success;

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

    public boolean isMovable(Object item, TreeItem targetTreeItem) {
        return (item instanceof ProjectFolder || item instanceof Folder) && item != getSource() && targetTreeItem != getSourceTreeItem().getParent();
    }

    public void dragDetectedEvent(Object value, TreeItem treeItem, MouseEvent event, TreeView treeView) {
        setSource(value);
        setSourceTreeItem(treeItem);
        if (value instanceof ProjectNode && treeItem != treeView.getRoot()) {
            Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cb = new ClipboardContent();
            cb.putString(((ProjectNode) value).getName());
            db.setContent(cb);
            event.consume();
        }
    }

    public void dragDetectedEvent(Object value, TreeItem treeItem, MouseEvent event, TreeTableView tree) {
        setSource(value);
        setSourceTreeItem(treeItem);
        if (value instanceof Project && treeItem != tree.getRoot()) {
            Dragboard db = tree.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cb = new ClipboardContent();
            cb.putString(((Project) value).getName());
            db.setContent(cb);
            event.consume();
        }
    }

    public void dragOverEvent(DragEvent event, Object item, TreeItem targetTreeItem, TreeCell treeCell) {
        if (isMovable(item, targetTreeItem) && !isSourceAncestorOf(targetTreeItem)) {
            boolean nameSearch = false;
            treeItemChildrenSize(targetTreeItem, nameSearch);
            textFillColor(treeCell);
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    public void dragOverEvent(DragEvent event, Object item, TreeTableRow treeTableRow, TreeTableCell treetableCell) {
        if (isMovable(item, treeTableRow.getTreeItem()) && !isSourceAncestorOf(treeTableRow.getTreeItem())) {
            boolean nameSearch = false;
            treeItemChildrenSize(treeTableRow.getTreeItem(), nameSearch);
            textFillColor(treetableCell);
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    public void dragDroppedEvent(Object value, TreeItem treeItem, DragEvent event, ProjectNode projectNode) {
        if (value instanceof ProjectFolder && value != getSource()) {
            ProjectFolder projectFolder = (ProjectFolder) projectNode;
            boolean search = false;
            success = false;
            treeItemChildrenSize(treeItem, search);
            accepTransferDrag(projectFolder, success);
            event.setDropCompleted(success);
            refresh(getSourceTreeItem().getParent());
            refresh(treeItem);
            event.consume();
        }
    }

    public void dragDroppedEvent(Object value, TreeItem treeItem, DragEvent event, Node node) {
        if (value instanceof Folder && value != getSource()) {
            Folder folder = (Folder) node;
            boolean search = false;
            success = false;
            treeItemChildrenSize(treeItem, search);
            accepTransferDrag(folder, success);
            event.setDropCompleted(success);
            refresh(getSourceTreeItem().getParent());
            refresh(treeItem);
            event.consume();
        }
    }

    private void textFillColor(TreeCell treeCell) {
        if (!nameExists()) {
            treeCell.setTextFill(Color.CHOCOLATE);
        }
    }

    private void textFillColor(TreeTableCell treeTableCell) {
        if (!nameExists()) {
            treeTableCell.setTextFill(Color.CHOCOLATE);
        }
    }


    private boolean nameExists() {
        return nameFound;
    }


    private boolean isSourceAncestorOf(TreeItem targetTreeItem) {
        TreeItem treeItemParent = targetTreeItem.getParent();
        while (treeItemParent != null) {
            if (getSourceTreeItem() == treeItemParent) {
                return true;
            } else {
                treeItemParent = treeItemParent.getParent();
            }

        }
        return false;
    }

    private void treeItemChildrenSize(TreeItem treeItem, boolean nameMatch) {
        nameFound = nameMatch;
        if (!treeItem.isLeaf()) {
            if (treeItem.getValue() instanceof ProjectFolder) {
                projectFolderChildrenSize(treeItem);
            } else if (treeItem.getValue() instanceof Folder) {
                folderChildrenSize(treeItem);
            }
        }
    }

    private void projectFolderChildrenSize(TreeItem treeItem) {
        ProjectFolder treeItemFolder = (ProjectFolder) treeItem.getValue();
        if (!treeItemFolder.getChildren().isEmpty()) {
            for (ProjectNode node : treeItemFolder.getChildren()) {
                if (node == null) {
                    break;
                } else if (node.getName().equals(getSource().toString())) {
                    nameFound = true;
                }
            }
        }
    }

    private void folderChildrenSize(TreeItem treeItem) {
        Folder folder = (Folder) treeItem.getValue();
        if (!folder.getChildren().isEmpty()) {
            for (Node node : folder.getChildren()) {
                if (node == null) {
                    break;
                } else if (node.getName().equals(getSource().toString())) {
                    nameFound = true;
                }
            }
        }
    }

    /**
     * @param projectFolder
     * @param s
     */
    private void accepTransferDrag(ProjectFolder projectFolder, boolean s) {
        success = s;
        if (nameExists()) {
            GseAlerts.showDraggingError();
        } else {
            ProjectNode monfichier = (ProjectNode) getSource();
            monfichier.moveTo(projectFolder);
            success = true;
        }
    }

    /**
     * @param folder
     * @param s
     */
    private void accepTransferDrag(Folder folder, boolean s) {
        success = s;
        if (nameExists()) {
            GseAlerts.showDraggingError();
        } else {
            Project monfichier = (Project) getSource();
            monfichier.moveTo(folder);
            success = true;
        }
    }

    private void refresh(TreeItem item) {
        if (item.getValue() instanceof ProjectFolder || item.getValue() instanceof Folder) {
            item.setExpanded(false);
            item.setExpanded(true);
        }
    }


}
