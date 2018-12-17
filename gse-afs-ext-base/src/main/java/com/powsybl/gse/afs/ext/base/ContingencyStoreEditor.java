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
import com.powsybl.gse.util.GseAlerts;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.*;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
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
            contingencyTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            setOnDragOver(this::onDragOver);
            setOnDragDropped(this::onDragDropped);
        }

        private void onDragOver(DragEvent event) {
            if (event.getGestureSource() != contingencyTree) {
                Dragboard db = event.getDragboard();
                if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                    acceptEquipmentInfo(event);
                }
            }
            event.consume();
        }

        private void acceptEquipmentInfo(DragEvent event) {
            List<EquipmentInfo> infos = (List<EquipmentInfo>) event.getDragboard().getContent(EquipmentInfo.DATA_FORMAT);

            // Test if all elements are convertible to a ContingencyElement
            Map<String, ContingencyElement> elements = infos.stream()
                    .map(ContingencyStoreEditor::createContingencyElement)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(ContingencyElement::getId, Function.identity()));
            if (elements.size() != infos.size()) {
                return;
            }

            // Clash if the elements have the same ID and different types
            Predicate<ContingencyElement> incompatibleTypePredicate = e -> elements.containsKey(e.getId()) && e.getType() != elements.get(e.getId()).getType();
            TreeItem<Object> contingencyItem = getContingencyItem();
            if (contingencyItem != null) {
                Contingency contingency = (Contingency) contingencyItem.getValue();
                if (contingency.getElements().stream().anyMatch(incompatibleTypePredicate)) {
                    return;
                }
            }

            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }

        private void onDragDropped(DragEvent event) {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                List<EquipmentInfo> infos = (List<EquipmentInfo>) db.getContent(EquipmentInfo.DATA_FORMAT);
                List<ContingencyElement> elements = infos.stream()
                        .map(ContingencyStoreEditor::createContingencyElement)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                addContingencyElements(elements);

                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        }

        private void addContingencyElements(List<ContingencyElement> elements) {
            TreeItem<Object> contingencyItem = getContingencyItem();

            if (contingencyItem != null) {
                Contingency contingency = (Contingency) contingencyItem.getValue();
                Set<String> ids = contingency.getElements().stream()
                        .map(ContingencyElement::getId)
                        .collect(Collectors.toSet());

                for (ContingencyElement e : elements) {
                    if (!ids.contains(e.getId())) {
                        contingency.addElement(e);
                        contingencyItem.getChildren().add(new TreeItem<>(e));
                        saved.set(false);
                    }
                }
            } else {
                addContingency(new Contingency(elements.get(0).getId(), elements));
            }
        }

        private void addContingency(Contingency contingency) {
            TreeItem<Object> contingencyItem = createTreeItem(contingency);
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

        private TreeItem<Object> getContingencyItem() {
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
    }

    private static TreeItem<Object> createTreeItem(Contingency contingency) {
        TreeItem<Object> contingencyItem = new TreeItem<>(contingency);
        for (ContingencyElement element : contingency.getElements()) {
            contingencyItem.getChildren().add(new TreeItem<>(element));
        }
        contingencyItem.setExpanded(true);
        return contingencyItem;
    }

    private static ContingencyElement createContingencyElement(EquipmentInfo equipmentInfo) {
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
        removeButton.setOnAction(event -> showRemoveAlert());
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
                contingencyTree.setContextMenu(createMultipleContingencyMenu());
            }
        });

        contingencyTree.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        setTop(toolBar);
        setCenter(contingencyTree);
    }

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.DELETE) {
            showRemoveAlert();
        } else if (e.getCode() == KeyCode.F2) {
            TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
            if (item.getValue() instanceof Contingency) {
                rename();
            }
        }
    }

    private void readContingencies() {
        root.getChildren().setAll(store.read().stream()
                .map(ContingencyStoreEditor::createTreeItem)
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
        removeItem.setOnAction(event -> showRemoveAlert());
        return new ContextMenu(renameItem, removeItem);
    }

    private ContextMenu createMultipleContingencyMenu() {
        MenuItem removeItem = new MenuItem(REMOVE);
        removeItem.setOnAction(event -> showRemoveAlert());
        return new ContextMenu(removeItem);
    }

    private void rename() {
        TreeItem<Object> item = contingencyTree.getSelectionModel().getSelectedItem();
        Contingency contingency = (Contingency) item.getValue();
        TextInputDialog dialog = new TextInputDialog(contingency.getId());
        dialog.setTitle(RESOURCE_BUNDLE.getString("RenameContingency"));
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
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

    private void showRemoveAlert() {
        ObservableList<TreeItem<Object>> selectedItems = contingencyTree.getSelectionModel().getSelectedItems();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(REMOVE);
        if (selectedItems.size() == 1) {
            TreeItem<Object> item = selectedItems.get(0);
            if (item.getValue() instanceof Contingency) {
                alert.setHeaderText(REMOVE + " " + ((Contingency) item.getValue()).getId() + " ?");
            } else if (item.getValue() instanceof ContingencyElement) {
                alert.setHeaderText(REMOVE + " " + ((ContingencyElement) item.getValue()).getId() + " ?");
            }
        } else {
            alert.setHeaderText(REMOVE + " " + RESOURCE_BUNDLE.getString("SelectedElements") + " ?");
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
        ObservableList<TreeItem<Object>> selectedItems = contingencyTree.getSelectionModel().getSelectedItems();
        List<TreeItem<Object>> items = new ArrayList<>(selectedItems);
        for (TreeItem<Object> item : items) {
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
        }
        saved.set(false);
    }

    private ContextMenu createContingencyElementMenu() {
        MenuItem removeItem = new MenuItem(REMOVE);
        removeItem.setOnAction(event -> showRemoveAlert());
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
    public SimpleBooleanProperty savedProperty() {
        return saved;
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
            return GseAlerts.showSaveDialog(store.getName(), this);
        }
        return true;
    }
}
