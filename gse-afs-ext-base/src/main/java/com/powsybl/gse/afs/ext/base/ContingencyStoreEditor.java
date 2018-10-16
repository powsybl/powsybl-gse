/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.contingency.*;
import com.powsybl.contingency.afs.ContingencyStore;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.spi.Savable;
import com.powsybl.gse.util.EquipmentInfo;
import com.powsybl.gse.util.Glyph;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ContingencyStoreEditor extends BorderPane implements ProjectFileViewer, Savable {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ContingencyStore");

    private final ContingencyStore store;

    private final TreeItem<Object> root = new TreeItem<>();

    private final TreeView<Object> contingencyTree = new TreeView<>(root);

    private final SimpleBooleanProperty saved = new SimpleBooleanProperty(true);

    private final class ContingencyTreeCell extends TreeCell<Object> {

        private ContingencyTreeCell() {
            setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                    EquipmentInfo equipmentInfo = (EquipmentInfo) db.getContent(EquipmentInfo.DATA_FORMAT);
                    ContingencyElement element = createElement(equipmentInfo);
                    if (element != null) {
                        addContingencyElement(element);
                    }
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        private void addContingencyElement(ContingencyElement element) {
            TreeItem<Object> contingencyItem = null;
            if (getTreeItem() != null) {
                if (getTreeItem().getValue() instanceof Contingency) {
                    contingencyItem = getTreeItem();
                } else if (getTreeItem().getValue() instanceof ContingencyElement) {
                    contingencyItem = getTreeItem().getParent();
                }
            }
            if (contingencyItem != null) {
                Contingency contingency = (Contingency) contingencyItem.getValue();
                if (contingency.getElements().stream().noneMatch(element2 -> element2.getId().equals(element.getId()))) {
                    contingency.addElement(element);
                    contingencyItem.getChildren().add(new TreeItem<>(element));
                }
            } else {
                addContingency(new Contingency(element.getId(), element));
            }
        }

        private void addContingency(Contingency contingency) {
            TreeItem<Object> contingencyItem = createItem(contingency);
            root.getChildren().add(contingencyItem);
            saved.set(false);
        }

        @Override
        public void updateItem(Object value, boolean empty) {
            super.updateItem(value, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (value instanceof Contingency) {
                    setText(((Contingency) value).getId());
                } else if (value instanceof ContingencyElement) {
                    setText(((ContingencyElement) value).getId());
                }
            }
        }
    }

    private static TreeItem<Object> createItem(Contingency contingency) {
        TreeItem<Object> contingencyItem = new TreeItem<>(contingency);
        for (ContingencyElement element : contingency.getElements()) {
            contingencyItem.getChildren().add(new TreeItem<>(element));
        }
        contingencyItem.setExpanded(true);
        return contingencyItem;
    }

    private ContingencyElement createElement(EquipmentInfo equipmentInfo) {
        switch (equipmentInfo.getType()) {
            case "BUSBAR_SECTION":
                return new BusbarSectionContingency(equipmentInfo.getIdAndName().getId());

            case "GENERATOR":
                return new GeneratorContingency(equipmentInfo.getIdAndName().getId());

            case "HVDC_LINE":
                return new HvdcLineContingency(equipmentInfo.getIdAndName().getId());

            case "LINE":
            case "TWO_WINDINGS_TRANSFORMER":
                return new BranchContingency(equipmentInfo.getIdAndName().getId());

            default:
                return null;
        }
    }

    public ContingencyStoreEditor(ContingencyStore store) {
        this.store = Objects.requireNonNull(store);

        Text saveGlyph = Glyph.createAwesomeFont('\uf0c7').size("1.3em");
        Button saveButton = new Button("", saveGlyph);
        saveButton.getStyleClass().add("gse-toolbar-button");
        saveButton.disableProperty().bind(saved);
        saveButton.setOnAction(event -> save());
        Text removeGlyph = Glyph.createAwesomeFont('\uf1f8').size("1.3em");
        Button removeButton = new Button("", removeGlyph);
        removeButton.getStyleClass().add("gse-toolbar-button");
        removeButton.disableProperty().bind(Bindings.isEmpty(contingencyTree.getSelectionModel().getSelectedIndices()));
        removeButton.setOnAction(event -> remove());
        ToolBar toolBar = new ToolBar(saveButton, removeButton);

        contingencyTree.setCellFactory(param -> new ContingencyTreeCell());
        contingencyTree.setShowRoot(false);
        contingencyTree.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (event.getGestureSource() != contingencyTree &&
                    db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                EquipmentInfo equipmentInfo = (EquipmentInfo) db.getContent(EquipmentInfo.DATA_FORMAT);
                ContingencyElement element = createElement(equipmentInfo);
                if (element != null) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });

        ContextMenu contingencyMenu = createContingencyMenu();
        ContextMenu contingencyElementMenu = createContingencyElementMenu();

        contingencyTree.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Object>>) c -> {
            if (c.getList().size() == 1) {
                TreeItem<Object> selectedTreeItem = c.getList().get(0);
                Object value = selectedTreeItem.getValue();
                if (value instanceof Contingency) {
                    contingencyTree.setContextMenu(contingencyMenu);
                } else if (value instanceof ContingencyElement) {
                    contingencyTree.setContextMenu(contingencyElementMenu);
                } else {
                    contingencyTree.setContextMenu(null);
                }
            } else {
                contingencyTree.setContextMenu(null);
            }
        });

        setTop(toolBar);
        setCenter(contingencyTree);
    }

    private void readContingencies() {
        root.getChildren().setAll(store.read().stream()
                .map(ContingencyStoreEditor::createItem)
                .collect(Collectors.toList()));
        root.setExpanded(true);
    }

    private void writeContingencies() {
        store.write(root.getChildren().stream()
                .map(item -> (Contingency) item.getValue())
                .collect(Collectors.toList()));
    }

    private ContextMenu createContingencyMenu() {
        MenuItem renameItem = new MenuItem(RESOURCE_BUNDLE.getString("Rename") + "...");
        renameItem.setOnAction(event -> rename());
        MenuItem removeItem = new MenuItem(RESOURCE_BUNDLE.getString("Remove"));
        removeItem.setOnAction(event -> remove());
        return new ContextMenu(renameItem, removeItem);
    }

    private void rename() {
        TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
        Contingency contingency = (Contingency) item.getValue();
        TextInputDialog dialog = new TextInputDialog(contingency.getId());
        dialog.setTitle(RESOURCE_BUNDLE.getString("RenameContingency"));
        dialog.setHeaderText(RESOURCE_BUNDLE.getString("NewName"));
        dialog.setContentText(RESOURCE_BUNDLE.getString("Name"));
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.isEmpty()) {
                contingency.setId(newName);
                contingencyTree.refresh();
            }
        });
    }

    private void remove() {
        TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
        if (item.getValue() instanceof Contingency) {
            item.getParent().getChildren().remove(item);
        } else {
            Contingency contingency = (Contingency) item.getParent().getValue();
            if (contingency.getElements().size() == 1) {
                // remove the contingency to avoid empty contingencies
                item.getParent().getParent().getChildren().remove(item.getParent());
            } else {
                ContingencyElement element = (ContingencyElement) item.getValue();
                contingency.removeElement(element);
                item.getParent().getChildren().remove(item);
            }
        }
        saved.set(false);
    }

    private ContextMenu createContingencyElementMenu() {
        MenuItem removeItem = new MenuItem(RESOURCE_BUNDLE.getString("Remove"));
        removeItem.setOnAction(event -> remove());
        return new ContextMenu(removeItem);
    }

    @Override
    public void save() {
        if (!saved.get()) {
            writeContingencies();
            saved.set(true);
        }
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public void view() {
        readContingencies();
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
