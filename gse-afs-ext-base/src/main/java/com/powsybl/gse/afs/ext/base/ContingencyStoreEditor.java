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
import com.powsybl.gse.util.IdAndName;
import com.powsybl.gse.util.GseAlerts;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.*;

import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ContingencyStoreEditor extends BorderPane implements ProjectFileViewer, Savable {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ContingencyStore");

    private static final String REMOVE = RESOURCE_BUNDLE.getString("Remove");

    private final ContingencyStore store;

    private final TreeItem<Object> root = new TreeItem<>();

    private final TreeView<Object> contingencyTree = new TreeView<>(root);

    private final SimpleBooleanProperty saved = new SimpleBooleanProperty(true);

    private final class ContingencyTreeCell extends TreeCell<Object> {

        private ContingencyTreeCell() {
            setOnDragDetected(this::onDragDetected);
            setOnDragOver(this::onDragOver);
            setOnDragDropped(this::onDragDropped);
        }

        private TreeItem<Object> getContingencyTreeItem() {
            TreeItem<Object> contingencyItem = null;
            if (getTreeItem() != null) {
                if (getTreeItem().getValue() instanceof Contingency) {
                    contingencyItem = getTreeItem();
                } else if (getTreeItem().getValue() instanceof ContingencyElement) {
                    contingencyItem = getTreeItem().getParent();
                }
            }
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
                case "BRANCH":
                    return new BranchContingency(equipmentInfo.getIdAndName().getId());

                default:
                    return null;
            }
        }

        private EquipmentInfo createEquipmentInfo(ContingencyElement contingencyElement) {
            ContingencyElementType type = contingencyElement.getType();
            if (type.equals(ContingencyElementType.BUSBAR_SECTION)) {
                return new EquipmentInfo(new IdAndName(contingencyElement.getId(), contingencyElement.getId()), "BUSBAR_SECTION");
            } else if (type.equals(ContingencyElementType.GENERATOR)) {
                return new EquipmentInfo(new IdAndName(contingencyElement.getId(), contingencyElement.getId()), "GENERATOR");
            } else if (type.equals(ContingencyElementType.HVDC_LINE)) {
                return new EquipmentInfo(new IdAndName(contingencyElement.getId(), contingencyElement.getId()), "HVDC_LINE");
            } else if (type.equals(ContingencyElementType.BRANCH)) {
                return new EquipmentInfo(new IdAndName(contingencyElement.getId(), contingencyElement.getId()), "BRANCH");
            } else {
                return null;
            }
        }

        private void onDragDetected(MouseEvent event) {
            TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
            if (item.getValue() instanceof ContingencyElement) {
                Dragboard db = startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(EquipmentInfo.DATA_FORMAT, createEquipmentInfo((ContingencyElement) item.getValue()));
                db.setContent(content);
                event.consume();
            }
        }

        private void onDragOver(DragEvent event) {
            Dragboard db = event.getDragboard();
            if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                acceptSingleDraggedElement(event);
            } else if (db.hasContent(EquipmentInfo.DATA_FORMAT_LIST)) {
                acceptMultipleDraggedElements(event);
            }
            event.consume();
        }

        private void onDragDropped(DragEvent event) {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                EquipmentInfo equipmentInfo = (EquipmentInfo) db.getContent(EquipmentInfo.DATA_FORMAT);
                ContingencyElement element = createElement(equipmentInfo);
                if (element != null) {
                    addContingencyElement(element);
                }
                success = true;
                if (event.getTransferMode().equals(TransferMode.MOVE) &&
                        event.getGestureSource() instanceof ContingencyTreeCell) {
                    remove();
                }
            } else if (db.hasContent(EquipmentInfo.DATA_FORMAT_LIST)) {
                List<EquipmentInfo> equipmentInfoList = (List<EquipmentInfo>) db.getContent(EquipmentInfo.DATA_FORMAT_LIST);
                for (EquipmentInfo equipmentInfo : equipmentInfoList) {
                    ContingencyElement element = createElement(equipmentInfo);
                    if (element != null) {
                        addContingencyElement(element);
                    }
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        }

        private void addContingencyElement(ContingencyElement element) {
            TreeItem<Object> contingencyItem = getContingencyTreeItem();
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

        private void acceptSingleDraggedElement(DragEvent event) {
            EquipmentInfo equipmentInfo = (EquipmentInfo) event.getDragboard().getContent(EquipmentInfo.DATA_FORMAT);
            ContingencyElement element = createElement(equipmentInfo);
            TreeItem<Object> contingencyTreeItem = getContingencyTreeItem();
            if (element != null) {
                if (contingencyTreeItem != null) {
                    Contingency contingency = (Contingency) contingencyTreeItem.getValue();
                    if (!contingency.getElements().contains(element)) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                } else {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
        }

        private void acceptMultipleDraggedElements(DragEvent event) {
            List<ContingencyElement> contingencyElementList = new ArrayList<>();
            List<EquipmentInfo> equipmentInfoList = (List<EquipmentInfo>) event.getDragboard().getContent(EquipmentInfo.DATA_FORMAT_LIST);
            for (EquipmentInfo equipmentInfo : equipmentInfoList) {
                ContingencyElement element = createElement(equipmentInfo);
                if (element != null) {
                    contingencyElementList.add(element);
                }
            }
            if (contingencyElementList.size() == equipmentInfoList.size()) {
                TreeItem<Object> contingencyTreeItem = getContingencyTreeItem();
                if (contingencyTreeItem != null) {
                    Contingency contingency = (Contingency) contingencyTreeItem.getValue();
                    if (!contingency.getElements().containsAll(contingencyElementList)) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                } else {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
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

        contingencyTree.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        setTop(toolBar);
        setCenter(contingencyTree);
    }

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.DELETE) {
            alertRemove();
        } else if (e.getCode() == KeyCode.F2) {
            TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
            if (item.getValue() instanceof Contingency) {
                rename();
            }
        }
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
        MenuItem removeItem = new MenuItem(REMOVE);
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
        TextField inputField = dialog.getEditor();
        BooleanBinding isInvalid = Bindings.createBooleanBinding(() -> inputField.getText().equals(contingency.getId()) || inputField.getText().isEmpty(),
                inputField.textProperty());
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(isInvalid);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            contingency.setId(newName);
            contingencyTree.refresh();
            saved.set(false);
        });
    }

    private void alertRemove() {
        TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(REMOVE);
        if (item.getValue() instanceof Contingency) {
            alert.setHeaderText(REMOVE + " " + ((Contingency) item.getValue()).getId() + " ?");
        } else if (item.getValue() instanceof ContingencyElement) {
            alert.setHeaderText(REMOVE + " " + ((ContingencyElement) item.getValue()).getId() + " ?");
        }
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(type -> {
            if (type == ButtonType.OK) {
                remove();
            } else {
                alert.close();
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
        MenuItem removeItem = new MenuItem(REMOVE);
        removeItem.setOnAction(event -> remove());
        return new ContextMenu(removeItem);
    }

    private boolean showSaveAlert() {
        Optional<ButtonType> result = GseAlerts.showSaveAndQuitDialog(store.getName());
        return result.map(type -> {
            if (type.getButtonData() == ButtonBar.ButtonData.YES) {
                save();
            }

            return type != ButtonType.CANCEL;
        }).orElse(false);
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
        //noting to dispose
    }

    @Override
    public boolean isClosable() {
        if (!saved.get()) {
            return showSaveAlert();
        }
        return true;
    }
}
