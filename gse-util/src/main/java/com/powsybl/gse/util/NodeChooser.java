/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.*;
import com.powsybl.gse.spi.GseContext;
import impl.org.controlsfx.skin.BreadCrumbBarSkin;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Callback;
import org.controlsfx.control.BreadCrumbBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeChooser<N, F extends N, D extends N, T extends N> extends GridPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeChooser.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NodeChooser");

    private static final String ICON_SIZE = "1.1em";

    private static class MoveContext {
        private Object source;
        private TreeItem sourceTreeItem;
        private TreeItem sourceparentTreeItem;
    }

    private MoveContext moveContext;
    private int counter;
    private boolean success;

    private MenuItem deleteMenuItem;

    private MenuItem renameMenuItem;

    public interface TreeModel<N, F, D> {
        Collection<N> getChildren(D folder);

        boolean isFolder(N node);

        boolean isWritable(D folder);

        String getName(N node);

        Collection<D> getRootFolders();

        D getFolder(String path);

        NodePath getPath(N node);

        Class<N> getNodeClass();

        String getLastSelectedPathKey();

        String getDescription(F file);

        Class<? extends F> getUnknownFileClass();

        Optional<D> showCreateFolderDialog(Window window, D parent);
    }

    private static class TreeModelImpl implements TreeModel<Node, File, Folder> {
        private final AppData appData;

        public TreeModelImpl(AppData appData) {
            this.appData = Objects.requireNonNull(appData);
        }

        @Override
        public Collection<Node> getChildren(Folder folder) {
            return folder.getChildren();
        }

        @Override
        public boolean isFolder(Node node) {
            return node != null ? node.isFolder() : true;
        }

        @Override
        public boolean isWritable(Folder folder) {
            return folder.isWritable();
        }

        @Override
        public String getName(Node node) {
            return node != null ? node.getName() : RESOURCE_BUNDLE.getString("scanning") + "...";
        }

        @Override
        public Collection<Folder> getRootFolders() {
            return appData.getFileSystems().stream().map(AppFileSystem::getRootFolder).collect(Collectors.toList());
        }

        @Override
        public Folder getFolder(String path) {
            return appData.getNode(path)
                    .filter(Node::isFolder)
                    .map(Folder.class::cast)
                    .orElse(null);
        }

        @Override
        public NodePath getPath(Node node) {
            return node.getPath();
        }

        @Override
        public Class<Node> getNodeClass() {
            return Node.class;
        }

        @Override
        public String getLastSelectedPathKey() {
            return "lastSelectedPath";
        }

        @Override
        public String getDescription(File file) {
            return file.getDescription();
        }

        @Override
        public Class<? extends File> getUnknownFileClass() {
            return UnknownFile.class;
        }

        @Override
        public Optional<Folder> showCreateFolderDialog(Window window, Folder parent) {
            return NewFolderPane.showAndWaitDialog(window, parent);
        }
    }

    private static class TreeModelProjectImpl implements TreeModel<ProjectNode, ProjectFile, ProjectFolder> {

        private final Project project;

        public TreeModelProjectImpl(Project project) {
            this.project = Objects.requireNonNull(project);
        }

        @Override
        public Collection<ProjectNode> getChildren(ProjectFolder folder) {
            return folder.getChildren();
        }

        @Override
        public boolean isFolder(ProjectNode projectNode) {
            return projectNode == null || projectNode.isFolder();
        }

        @Override
        public boolean isWritable(ProjectFolder projectFolder) {
            return true;
        }

        @Override
        public String getName(ProjectNode projectNode) {
            return projectNode != null ? projectNode.getName() : RESOURCE_BUNDLE.getString("scanning") + "...";
        }

        @Override
        public Collection<ProjectFolder> getRootFolders() {
            return Collections.singleton(project.getRootFolder());
        }

        @Override
        public ProjectFolder getFolder(String path) {
            return project.getRootFolder().getChild(path)
                    .filter(ProjectNode::isFolder)
                    .map(ProjectFolder.class::cast)
                    .orElse(null);
        }

        @Override
        public NodePath getPath(ProjectNode projectNode) {
            return projectNode.getPath();
        }

        @Override
        public Class<ProjectNode> getNodeClass() {
            return ProjectNode.class;
        }

        @Override
        public String getLastSelectedPathKey() {
            return "projectLastSelectedPath";
        }

        @Override
        public String getDescription(ProjectFile projectFile) {
            return "";
        }

        @Override
        public Class<? extends ProjectFile> getUnknownFileClass() {
            return UnknownProjectFile.class;
        }

        @Override
        public Optional<ProjectFolder> showCreateFolderDialog(Window window, ProjectFolder parent) {
            return NewFolderPane.showAndWaitDialog(window, parent);
        }
    }

    private final BreadCrumbBar<N> path = new BreadCrumbBar<>();
    private final Button createFolderButton;
    private final Button deleteFolderButton;
    private final TreeItem<N> rootItem = new TreeItem<>();
    private TreeTableView<N> tree = new TreeTableView<>(rootItem);
    private final ObjectProperty<T> selectedNode = new SimpleObjectProperty<>();
    private final ObjectProperty<D> selectedFolder = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty doubleClick = new SimpleBooleanProperty(false);
    private final Window window;
    private final BiFunction<N, TreeModel<N, F, D>, Boolean> filter;
    private final AppData appData;
    private final Preferences preferences;
    private final TreeModel<N, F, D> treeModel;
    private final GseContext context;

    public NodeChooser(Window window, TreeModel<N, F, D> treeModel, AppData appData, GseContext context,
                       BiFunction<N, TreeModel<N, F, D>, Boolean> filter) {
        this.window = Objects.requireNonNull(window);
        this.treeModel = Objects.requireNonNull(treeModel);
        this.appData = Objects.requireNonNull(appData);
        this.context = Objects.requireNonNull(context);
        this.filter = Objects.requireNonNull(filter);
        preferences = Preferences.userNodeForPackage(getClass());
        tree.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tree.setShowRoot(false);
        rootItem.getChildren().add(new TreeItem<>());
        TreeTableColumn<N, N> fileColumn = new TreeTableColumn<>(RESOURCE_BUNDLE.getString("File"));
        fileColumn.setPrefWidth(415);
        fileColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<N, N> callback) -> {
            N node = callback.getValue().getValue();
            return new ReadOnlyObjectWrapper<>(node);
        });
        fileColumn.setCellFactory(new Callback<TreeTableColumn<N, N>, TreeTableCell<N, N>>() {
            @Override
            public TreeTableCell<N, N> call(TreeTableColumn<N, N> param) {
                return new TreeTableCell<N, N>() {
                    @Override
                    protected void updateItem(N item, boolean empty) {
                        super.updateItem(item, empty);
                        updateTreeTableCellFileItem(item, empty, getTreeTableRow(), this);
                    }
                };
            }
        });
        TreeTableColumn<N, N> descriptionColumn = new TreeTableColumn<>(RESOURCE_BUNDLE.getString("Description"));
        descriptionColumn.setPrefWidth(182);
        descriptionColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<N, N> p) -> {
            N node = p.getValue().getValue();
            if (!treeModel.isFolder(node)) {
                return new ReadOnlyObjectWrapper<>(node);
            }
            return null;
        });
        descriptionColumn.setCellFactory(new Callback<TreeTableColumn<N, N>, TreeTableCell<N, N>>() {
            @Override
            public TreeTableCell<N, N> call(TreeTableColumn<N, N> param) {
                return new TreeTableCell<N, N>() {
                    @Override
                    protected void updateItem(N item, boolean empty) {
                        super.updateItem(item, empty);
                        updateTreeTableCellDescriptionItem(item, empty, this);
                    }
                };
            }
        });
        tree.getColumns().setAll(fileColumn, descriptionColumn);
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tree.getSelectionModel().getSelectedItems().addListener(this::treeViewChangeListener);
        tree.setOnMouseClicked(this::onMouseClickedEvent);
        ScrollPane scrollPane = new ScrollPane(tree);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        path.setCrumbFactory(item -> {
            if (item.getValue() != null) {
                return new BreadCrumbBarSkin.BreadCrumbButton(item.getValue().toString(), NodeGraphics.getGraphic(item.getValue()));
            } else {
                return new BreadCrumbBarSkin.BreadCrumbButton("");
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> path.setSelectedCrumb(newValue));
        path.setOnCrumbAction(event -> {
            tree.getSelectionModel().select(event.getSelectedCrumb());
            int index = tree.getRow(event.getSelectedCrumb());
            tree.scrollTo(index);
            tree.layout();
        });
        javafx.scene.Node createFolderGlyph = Glyph.createAwesomeFont('\uf07b').size("1.3em").color("#FFDB69")
                .stack(Glyph.createAwesomeFont('\uf055').color("limegreen").size("0.8em"));
        createFolderButton = new Button("", createFolderGlyph);
        createFolderButton.setPadding(new Insets(3, 5, 3, 5));
        createFolderButton.disableProperty().bind(selectedFolder.isNull());
        createFolderButton.setOnAction(event ->
                treeModel.showCreateFolderDialog(window, selectedFolder.get()).ifPresent(newFolder -> {
                    TreeItem<N> selectedItem = tree.getSelectionModel().getSelectedItem();
                    refresh(selectedItem);
                })
        );
        javafx.scene.Node deleteFolderGlyph = Glyph.createAwesomeFont('\uf1f8').size(ICON_SIZE);
        deleteFolderButton = new Button("", deleteFolderGlyph);
        deleteFolderButton.setPadding(new Insets(3, 5, 3, 5));
        deleteFolderButton.disableProperty().bind(selectedNode.isNull());
        ObservableList<TreeItem<N>> selectedItems = tree.getSelectionModel().getSelectedItems();
        deleteFolderButton.setOnAction(event -> createDeleteAlert(selectedItems));
        setHgap(5);
        setVgap(5);
        ColumnConstraints column0 = new ColumnConstraints();
        column0.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0);
        RowConstraints row0 = new RowConstraints();
        RowConstraints row1 = new RowConstraints();
        row1.setVgrow(Priority.ALWAYS);
        getRowConstraints().addAll(row0, row1);
        add(new ScrollPane(path), 0, 0);
        add(new HBox(5, createFolderButton, deleteFolderButton), 1, 0);
        add(scrollPane, 0, 1, 2, 1);
        context.getExecutor().submit(() -> {
            try {
                List<TreeItem<N>> nodes = new ArrayList<>();
                for (D rootFolder : treeModel.getRootFolders()) {
                    TreeItem<N> node = createCollapsedFolderItem(rootFolder);
                    node.setExpanded(true);
                    nodes.add(node);
                }
                Platform.runLater(() -> {
                    rootItem.getChildren().setAll(nodes);
                    // select first root
                    if (!nodes.isEmpty()) {
                        tree.getSelectionModel().select(nodes.get(0));
                    }
                    // select saved path
//                    String lastSelectedPath = preferences.get(treeModel.getLastSelectedPathKey(), null);
//                    if (lastSelectedPath != null) {
//                        D selectedFolder = treeModel.getFolder(lastSelectedPath);
//                        if (selectedFolder != null) {
//                            selectNode(treeModel.getPath(selectedFolder).toList());
//                        }
//                    }
                });
            } catch (Throwable t) {
                LOGGER.error(t.toString(), t);
            }
        });
    }

    public ReadOnlyObjectProperty<T> selectedNodeProperty() {
        return selectedNode;
    }

    public ReadOnlyBooleanProperty doubleClick() {
        return doubleClick;
    }

    private void refresh(TreeItem<N> item) {
        N node = item.getValue();
        if (treeModel.isFolder(node)) {
            try {
                List<TreeItem<N>> childItems = new ArrayList<>();
                for (N child : treeModel.getChildren((D) node)) {
                    if (treeModel.isFolder(child)) {
                        childItems.add(createCollapsedFolderItem((D) child));
                    } else {
                        F file = (F) child;
                        if (filter.apply(file, treeModel)) {
                            childItems.add(new TreeItem<>(child));
                        }
                    }
                }
                Platform.runLater(() -> item.getChildren().setAll(childItems));
            } catch (Throwable t) {
                LOGGER.error(t.toString(), t);
            }
        }
    }

    private Alert nameAlreadyExistsAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RESOURCE_BUNDLE.getString("DragError"));
        alert.setHeaderText(RESOURCE_BUNDLE.getString("Error"));
        alert.setContentText(RESOURCE_BUNDLE.getString("DragFileExists"));
        alert.showAndWait();
        return alert;
    }

    private void onMouseClickedEvent(MouseEvent event) {
        TreeItem<N> item = tree.getSelectionModel().getSelectedItem();
        N node = item != null ? item.getValue() : null;
        selectedNode.setValue(node != null && filter.apply(node, treeModel) ? (T) node : null);
        selectedFolder.setValue(node != null && treeModel.isFolder(node) && treeModel.isWritable((D) node) ? (D) node : null);
        doubleClick.setValue(event.getClickCount() == 2);
    }

    private void updateTreeTableCellFileItem(N item, boolean empty, TreeTableRow<N> treeTableRow, TreeTableCell<N, N> treeTableCell) {
        if (empty) {
            treeTableCell.setText(null);
            treeTableCell.setGraphic(null);
        } else {
            updateNonNullItemm(item, treeTableRow, treeTableCell);
        }
    }

    private void updateTreeTableCellDescriptionItem(N item, boolean empty, TreeTableCell<N, N> treeTableCell) {
        if (empty || item == null) {
            treeTableCell.setText(null);
        } else {
            treeTableCell.setText(treeModel.getDescription((F) item));
            treeTableCell.setOpacity(item.getClass() == treeModel.getUnknownFileClass() ? 0.5 : 1);
        }
    }

    private void updateNonNullItemm(N item, TreeTableRow<N> treeTableRow, TreeTableCell<N, N> treeTableCell) {
        if (item == null) {
            GseUtil.setWaitingText(treeTableCell);
        } else if (item instanceof Node) {
            Node node = (Node) item;
            treeTableCell.setText(treeModel.getName(item));
            treeTableCell.setTextFill(Color.BLACK);
            treeTableCell.setOpacity(item.getClass() == treeModel.getUnknownFileClass() ? 0.5 : 1);
            treeTableCell.setGraphic(NodeGraphics.getGraphic(item));
            treeTableCell.setOnDragDetected(event -> dragDetectedEvent(item, treeTableRow.getTreeItem(), event));
            treeTableCell.setOnDragOver(event -> dragOverEvent(event, item, treeTableRow, treeTableCell));
            treeTableCell.setOnDragDropped(event -> dragDroppedEvent(item, treeTableRow.getTreeItem(), event, node));
            treeTableCell.setOnDragExited(event -> treeTableCell.setTextFill(Color.BLACK));
        } else {
            treeTableCell.setText(treeModel.getName(item));
            treeTableCell.setTextFill(Color.BLACK);
            treeTableCell.setOpacity(item.getClass() == treeModel.getUnknownFileClass() ? 0.5 : 1);
            treeTableCell.setGraphic(NodeGraphics.getGraphic(item));
        }
    }

    private void textFillColor(TreeTableCell<N, N> treetableCell) {
        if (getCounter() < 1) {
            treetableCell.setTextFill(Color.CHOCOLATE);
        }
    }

    private void dragOverEvent(DragEvent event, Object item, TreeTableRow<N> treeTableRow, TreeTableCell<N, N> treetableCell) {
        if (item instanceof Folder && item != moveContext.source) {
            int count = 0;
            treeItemChildrenSize(treeTableRow.getTreeItem(), count);
            textFillColor(treetableCell);
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    public int getCounter() {
        return counter;
    }

    private void dragDetectedEvent(N value, TreeItem<N> treeItem, MouseEvent event) {
        moveContext = new MoveContext();
        moveContext.source = value;
        moveContext.sourceTreeItem = treeItem;
        moveContext.sourceparentTreeItem = moveContext.sourceTreeItem.getParent();
        if (value instanceof Project && treeItem != tree.getRoot()) {
            Dragboard db = tree.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cb = new ClipboardContent();
            cb.putString(((Project) value).getName());
            db.setContent(cb);
            event.consume();
        }
    }

    private void dragDroppedEvent(Object value, TreeItem<N> treeItem, DragEvent event, Node node) {
        if (value instanceof Folder && value != moveContext.source) {
            Folder folder = (Folder) node;
            int count = 0;
            success = false;
            treeItemChildrenSize(treeItem, count);
            accepTransferDrag(folder, success);
            event.setDropCompleted(success);
            refreshTreeItem(moveContext.sourceparentTreeItem);
            refreshTreeItem(treeItem);
            event.consume();
        }
    }

    private void refreshTreeItem(TreeItem<N> item) {
        if (item.getValue() instanceof Folder) {
            item.setExpanded(false);
            item.setExpanded(true);
        }
    }

    private void treeItemChildrenSize(TreeItem<N> treeItem, int compte) {
        counter = compte;
        if (!treeItem.isLeaf()) {
            Folder folder = (Folder) treeItem.getValue();
            if (!folder.getChildren().isEmpty()) {
                for (Node node : folder.getChildren()) {
                    if (node == null) {
                        break;
                    } else if (node.getName().equals(moveContext.source.toString())) {
                        counter++;
                    }
                }
            }
        }
    }

    private void accepTransferDrag(Folder folder, boolean s) {
        success = s;
        if (getCounter() >= 1) {
            nameAlreadyExistsAlert();
        } else if (getCounter() < 1) {
            Project monfichier = (Project) moveContext.source;
            monfichier.moveTo(folder);
            success = true;
        }
    }

    private void treeViewChangeListener(ListChangeListener.Change<? extends TreeItem<N>> c) {
        if (c.getList().isEmpty()) {
            tree.setContextMenu(null);
        } else if (c.getList().size() == 1) {
            TreeItem<N> selectedTreeItem = c.getList().get(0);
            N value = selectedTreeItem.getValue();
            if (value instanceof Project) {
                tree.setContextMenu(createProjectContextMenu(selectedTreeItem));
                tree.setOnKeyPressed((KeyEvent ke) -> {
                    if (ke.getCode() == KeyCode.F2) {
                        renameTextInputDialog();
                    }
                });

            } else if (value instanceof Folder) {
                tree.setContextMenu(createFolderContextMenu(selectedTreeItem));
            } else {
                tree.setContextMenu(null);
            }
        } else {
            tree.setContextMenu(createMultipleContextMenu(c.getList()));
        }
    }

    private ContextMenu createMultipleContextMenu(List<? extends TreeItem<N>> selectedTreeItems) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(createDeleteNodeMenuItem(selectedTreeItems));
        return contextMenu;
    }

    private ContextMenu createFolderContextMenu(TreeItem<N> selectedTreeItem) {
        ContextMenu contextMenu = new ContextMenu();
        List<MenuItem> items = new ArrayList<>();
        items.add(createRenameProjectMenuItem());
        items.add(createCreateFolderMenuItem());
        items.add(createDeleteNodeMenuItem(Collections.singletonList(selectedTreeItem)));
        contextMenu.getItems().addAll(items.stream()
                .sorted(Comparator.comparing(MenuItem::getText))
                .collect(Collectors.toList()));
        return contextMenu;
    }

    private ContextMenu createProjectContextMenu(TreeItem<N> selectedTreeItem) {
        ContextMenu contextMenu = new ContextMenu();
        List<MenuItem> items = new ArrayList<>();
        items.add(createDeleteNodeMenuItem(Collections.singletonList(selectedTreeItem)));
        items.add(createRenameProjectMenuItem());
        contextMenu.getItems().addAll(items.stream()
                .sorted(Comparator.comparing(MenuItem::getText))
                .collect(Collectors.toList()));
        return contextMenu;
    }

    private MenuItem createCreateFolderMenuItem() {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("CreateFolder") + "...", Glyph.createAwesomeFont('\uf115').size(ICON_SIZE));
        menuItem.setOnAction(event ->
                treeModel.showCreateFolderDialog(window, selectedFolder.get()).ifPresent(newFolder -> {
                    TreeItem<N> selectedItem = tree.getSelectionModel().getSelectedItem();
                    refreshTreeItem(selectedItem);
                })
        );
        return menuItem;
    }

    private MenuItem createRenameProjectMenuItem() {
        renameMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Rename"), Glyph.createAwesomeFont('\uf120').size(ICON_SIZE));
        renameMenuItem.setOnAction(event -> renameTextInputDialog());
        return renameMenuItem;
    }

    private void renameTextInputDialog() {
        TextInputDialog dialog = new TextInputDialog(tree.getSelectionModel().getSelectedItem().getValue().toString());
        dialog.setTitle(RESOURCE_BUNDLE.getString("RenameFile"));
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText(RESOURCE_BUNDLE.getString("Name"));
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newname -> {
            TreeItem<N> selectedTreeItem = tree.getSelectionModel().getSelectedItem();
            if (selectedTreeItem.getValue() instanceof Node) {
                Node localSelectednode = (Node) selectedTreeItem.getValue();
                localSelectednode.rename(newname);
                refreshTreeItem(selectedTreeItem.getParent());
                tree.getSelectionModel().clearSelection();
                tree.getSelectionModel().select(selectedTreeItem.getParent());
            }
        });

    }

    private MenuItem createDeleteNodeMenuItem(List<? extends TreeItem<N>> selectedTreeItems) {
        deleteMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Delete"), Glyph.createAwesomeFont('\uf1f8').size(ICON_SIZE));
        deleteMenuItem.setOnAction(event -> createDeleteAlert(selectedTreeItems));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        return deleteMenuItem;
    }

    private void deleleFolder(Folder folder) {
        if (folder.getChildren().isEmpty()) {
            folder.delete();
        } else if (!folder.getChildren().isEmpty()) {
            Alert deleteAlert = new Alert(Alert.AlertType.ERROR);
            deleteAlert.setTitle(RESOURCE_BUNDLE.getString("DeleteError"));
            deleteAlert.setHeaderText(RESOURCE_BUNDLE.getString("Error"));
            deleteAlert.setContentText(String.format(RESOURCE_BUNDLE.getString("DeletedFolderContainsProjects"), folder.toString()));
            deleteAlert.showAndWait();
        }
    }

    public void createDeleteAlert(List<? extends TreeItem<N>> selectedTreeItems) {
        GseAlerts.deleteNodesAlert(selectedTreeItems).showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                setOnOkButton(selectedTreeItems);
            }
        });
    }

    private void setOnOkButton(List<? extends TreeItem<N>> selectedTreeItems) {
        List<TreeItem<N>> parentTreeItems = new ArrayList<>();
        for (TreeItem<N> selectedTreeItem : selectedTreeItems) {
            if (selectedTreeItem.getValue() instanceof Project) {
                Project selectedProject = (Project) selectedTreeItem.getValue();
                selectedProject.delete();
            } else if (selectedTreeItem.getValue() instanceof Folder) {
                Folder folderSelected = (Folder) selectedTreeItem.getValue();
                deleleFolder(folderSelected);
            }
            parentTreeItems.add(selectedTreeItem.getParent());
        }
        for (TreeItem<N> parentTreeItem : parentTreeItems) {
            refreshTreeItem(parentTreeItem);
            tree.getSelectionModel().clearSelection();
            tree.getSelectionModel().select(parentTreeItem);
            selectedNode.setValue((T) parentTreeItem.getValue());
        }
    }

    private TreeItem<N> createCollapsedFolderItem(D folder) {
        TreeItem<N> item = new TreeItem<>(folder);
        item.getChildren().setAll(new TreeItem<>()); // dummy leaf
        item.setExpanded(false);
        item.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                context.getExecutor().submit(() -> refresh(item));
            }
        });
        return item;
    }

    private void selectNode(List<String> pathList) {
        selectNode(tree.getRoot(), new ArrayList<>(pathList));
    }

    private void scrollToLastItem(TreeItem<N> last) {
        // select and scroll the last item
        tree.getSelectionModel().select(last);
        int selectedIndex = tree.getSelectionModel().getSelectedIndex();
        tree.scrollTo(selectedIndex);
        tree.requestFocus();
    }

    private void selectNode(TreeItem<N> last, List<String> pathList) {
        last.setExpanded(true);
        if (!pathList.isEmpty()) {
            String name = pathList.remove(0);
            ObservableList<TreeItem<N>> children = last.getChildren();
            if (children.size() == 1 && children.get(0).getValue() == null) {
                // scanning ongoing
                // wait for scanning end
                children.addListener(new ListChangeListener<TreeItem<N>>() {
                    @Override
                    public void onChanged(Change<? extends TreeItem<N>> c) {
                        if (c.next() && c.wasReplaced() && c.getRemovedSize() == 1 && c.getRemoved().get(0).getValue() == null) {
                            TreeItem<N> next = null;
                            for (TreeItem<N> addedNode : c.getAddedSubList()) {
                                if (treeModel.getName(addedNode.getValue()).equals(name)) {
                                    next = addedNode;
                                    break;
                                }
                            }
                            if (next != null) {
                                selectNode(next, pathList);
                            } else {
                                scrollToLastItem(last);
                            }
                        }
                        c.getList().removeListener(this);
                    }
                });
            } else {
                boolean found = false;
                for (TreeItem<N> item : children) {
                    if (treeModel.getName(item.getValue()).equals(name)) {
                        selectNode(item, pathList);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    scrollToLastItem(last);
                }
            }
        } else {
            scrollToLastItem(last);
        }
    }

    private void savePreferences() {
        N node = selectedNode.get();
        if (node != null) {
            preferences.put(treeModel.getLastSelectedPathKey(), treeModel.getPath(node).toString());
        }
    }

    public static <T extends Node> Optional<T> showAndWaitDialog(Window window, AppData appData, GseContext context,
                                                                 BiFunction<Node, TreeModel<Node, File, Folder>, Boolean> filter) {
        return showAndWaitDialog(new TreeModelImpl(appData), window, appData, context, filter);
    }

    private static <N, T extends N> boolean testNode(N node, Class<T> filter, Class<?>... otherFilters) {
        if (!filter.isAssignableFrom(node.getClass())) {
            return false;
        }
        for (Class<?> otherFilter : otherFilters) {
            if (!otherFilter.isAssignableFrom(node.getClass())) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Node> Optional<T> showAndWaitDialog(Window window, AppData appData, GseContext context, Class<T> filter, Class<?>... otherFilters) {
        return showAndWaitDialog(new TreeModelImpl(appData), window, appData, context, (node, treeModel) -> testNode(node, filter, otherFilters));
    }

    public static <T extends ProjectNode> Optional<T> showAndWaitDialog(Project project, Window window, GseContext context, BiFunction<ProjectNode, TreeModel<ProjectNode, ProjectFile, ProjectFolder>, Boolean> filter) {
        return showAndWaitDialog(new TreeModelProjectImpl(project), window, project.getFileSystem().getData(), context, filter);
    }

    public static <T extends ProjectNode> Optional<T> showAndWaitDialog(Project project, Window window, GseContext context, Class<T> filter,
                                                                        Class<?>... otherFilters) {
        return showAndWaitDialog(new TreeModelProjectImpl(project), window, project.getFileSystem().getData(), context, (projectNode, treeModel) -> testNode(projectNode, filter, otherFilters));
    }

    public static <N, F extends N, D extends N, T extends N> Optional<T> showAndWaitDialog(
            TreeModel<N, F, D> treeModel, Window window, AppData appData, GseContext context,
            BiFunction<N, TreeModel<N, F, D>, Boolean> filter) {
        Dialog<T> dialog = new Dialog<>();
        try {
            dialog.setTitle(RESOURCE_BUNDLE.getString("OpenFile"));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            NodeChooser<N, F, D, T> nodeChooser = new NodeChooser<>(window, treeModel, appData, context, filter);
            Button button = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            button.disableProperty().bind(nodeChooser.selectedNodeProperty().isNull());
            nodeChooser.doubleClick().addListener((observable, oldValue, newValue) -> {
                if (Boolean.TRUE.equals(newValue)) {
                    button.fire();
                }
            });
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return nodeChooser.selectedNodeProperty().get();
                }
                return null;
            });
            dialog.getDialogPane().setContent(nodeChooser);
            dialog.setResizable(true);
            dialog.initOwner(window);
            Optional<T> node = dialog.showAndWait();
            nodeChooser.savePreferences();
            return node;
        } finally {
            dialog.close();
        }
    }
}
