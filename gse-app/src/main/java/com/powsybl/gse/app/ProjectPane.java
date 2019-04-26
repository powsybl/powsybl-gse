/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.panemu.tiwulfx.control.DetachableTabPane;
import com.powsybl.afs.*;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.spi.*;
import com.powsybl.gse.util.*;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ProjectPane extends Tab {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectPane.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ProjectPane");

    private static final ServiceLoaderCache<ProjectFileCreatorExtension> CREATOR_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileCreatorExtension.class);

    private static final ServiceLoaderCache<ProjectFileEditorExtension> EDITOR_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileEditorExtension.class);

    private static final ServiceLoaderCache<ProjectFileViewerExtension> VIEWER_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileViewerExtension.class);

    private static final ServiceLoaderCache<ProjectFileExecutionTaskExtension> EXECUTION_TASK_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileExecutionTaskExtension.class);

    private final KeyCombination saveKeyCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

    private static class TabKey {

        private final String nodeId;

        private final Class<?> viewerClass;

        public TabKey(String nodeId, Class<?> viewerClass) {
            this.nodeId = Objects.requireNonNull(nodeId);
            this.viewerClass = Objects.requireNonNull(viewerClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, viewerClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TabKey) {
                TabKey other = (TabKey) obj;
                return nodeId.equals(other.nodeId) && viewerClass.equals(other.viewerClass);
            }
            return false;
        }
    }

    public static class MyTab extends Tab {

        private ProjectFileViewer viewer;

        private final KeyCombination closeKeyCombination = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);

        private final KeyCombination closeAllKeyCombination = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

        public MyTab(String text, ProjectFileViewer viewer) {
            super(text, viewer.getContent());
            this.viewer = viewer;
            setContextMenu(contextMenu());
        }

        private ContextMenu contextMenu() {
            MenuItem closeMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Close"));
            MenuItem closeAllMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("CloseAll"));
            closeMenuItem.setOnAction(event -> closeTab(event, this));
            closeAllMenuItem.setOnAction(event -> {
                List<MyTab> mytabs = new ArrayList<>(getTabPane().getTabs().stream()
                        .map(tab -> (MyTab) tab)
                        .collect(Collectors.toList()));
                mytabs.forEach(mytab -> closeTab(event, mytab));
            });
            closeMenuItem.setAccelerator(closeKeyCombination);
            closeAllMenuItem.setAccelerator(closeAllKeyCombination);
            return new ContextMenu(closeMenuItem, closeAllMenuItem);
        }

        public ProjectFileViewer getViewer() {
            return viewer;
        }

        public void requestClose() {
            getTabPane().getTabs().remove(this);
            if (getOnClosed() != null) {
                Event.fireEvent(this, new Event(Tab.CLOSED_EVENT));
            }
        }

        private static void closeTab(Event event, MyTab tab) {
            if (!tab.getViewer().isClosable()) {
                event.consume();
            } else {
                tab.getTabPane().getTabs().remove(tab);
                tab.getViewer().dispose();
            }
        }
    }

    private boolean success;

    private DragAndDropMove dragAndDropMove;

    private final Project project;

    private final GseContext context;

    private final TreeView<Object> treeView;

    private final StackPane viewPane;

    private final TaskItemList taskItems;

    private final TaskMonitorPane taskMonitorPane;

    private static final String STAR_NOTIFICATION = " *";

    private static class CreationTaskList {

        private final Multimap<String, String> tasks = HashMultimap.create();

        private final Lock taskLock = new ReentrantLock();

        private Collection<String> getTaskPreviewNames(ProjectFolder folder) {
            taskLock.lock();
            try {
                return tasks.get(folder.getId());
            } finally {
                taskLock.unlock();
            }
        }

        private void add(ProjectFolder folder, String taskPreviewName) {
            taskLock.lock();
            try {
                tasks.put(folder.getId(), taskPreviewName);
            } finally {
                taskLock.unlock();
            }
        }

        private void remove(ProjectFolder folder, String taskPreviewName) {
            taskLock.lock();
            try {
                tasks.remove(folder.getId(), taskPreviewName);
            } finally {
                taskLock.unlock();
            }
        }
    }

    private static class FloatingScene extends Scene {

        public FloatingScene(Parent root, double width, double height) {
            super(root, width, height);
        }
    }

    private void treeViewChangeListener(ListChangeListener.Change<? extends TreeItem<Object>> c) {
        if (c.getList().isEmpty()) {
            treeView.setContextMenu(null);
        } else if (c.getList().size() == 1) {
            TreeItem<Object> selectedTreeItem = c.getList().get(0);
            Object value = selectedTreeItem.getValue();
            treeView.setOnKeyPressed((KeyEvent ke) -> {
                if (ke.getCode() == KeyCode.F2) {
                    renameProjectNode(selectedTreeItem);
                }
            });
            if (value instanceof ProjectFolder) {
                treeView.setContextMenu(createFolderContextMenu(selectedTreeItem));
            } else if (value instanceof ProjectFile) {
                treeView.setContextMenu(createFileContextMenu(selectedTreeItem));
            } else {
                // TODO show contextual menu to reach advanced task status ?
                treeView.setContextMenu(null);
            }
        } else {
            treeView.setContextMenu(createMultipleContextMenu(c.getList()));
        }
    }

    private TreeCell<Object> treeViewCellFactory(TreeView<Object> item) {

        return new TreeCell<Object>() {

            private void setForItemObject(Object value) {
                if (value instanceof String) {
                    setText((String) value);
                    setGraphic(getTreeItem().getGraphic());
                    setTextFill(Color.BLACK);
                    setOpacity(1);
                } else if (value instanceof ProjectNode) {
                    ProjectNode node = (ProjectNode) value;
                    setText(node.getName());
                    setGraphic(getTreeItem().getGraphic());
                    setTextFill(Color.BLACK);
                    setOpacity(node instanceof UnknownProjectFile ? 0.5 : 1);
                    setOnDragDetected(event -> dragDetectedEvent(getItem(), getTreeItem(), event));
                    setOnDragOver(event -> dragOverEvent(event, getItem(), getTreeItem(), this));
                    setOnDragDropped(event -> dragDroppedEvent(getItem(), getTreeItem(), event, node));
                    setOnDragExited(event -> getStyleClass().removeAll("treecell-drag-over"));
                } else {
                    throw new AssertionError();
                }
            }

            private void updateNonEmptyItem(Object value) {
                fillCellInfosForObject(value, this, getTreeItem());
                if (value == null) {
                    GseUtil.setWaitingText(this);
                } else {
                    setForItemObject(value);
                }
            }

            @Override
            protected void updateItem(Object value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    updateNonEmptyItem(value);
                }
            }
        };
    }

    private void runDefaultActionAfterDoubleClick(TreeItem<Object> selectedTreeItem) {
        Objects.requireNonNull(selectedTreeItem);
        Object value = selectedTreeItem.getValue();
        if (value instanceof ProjectFile) {
            List<ProjectFileEditorExtension> editorExtensions = findEditorExtensions((ProjectFile) value);
            if (!editorExtensions.isEmpty()) {
                String tabName = getTabName(selectedTreeItem);
                showProjectItemEditorDialog((ProjectFile) value, editorExtensions.get(0), tabName);
            } else {
                List<ProjectFileViewerExtension> viewerExtensions = findViewerExtensions((ProjectFile) value);
                if (!viewerExtensions.isEmpty()) {
                    ProjectFileViewerExtension viewerExtension = viewerExtensions.get(0);
                    String tabName = getTabName(selectedTreeItem);
                    viewFile((ProjectFile) value, viewerExtension, tabName);
                }
            }
        } else {
            // TODO show advanced task status ?
        }
    }

    private void treeViewMouseClickHandler(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            TreeItem<Object> selectedTreeItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedTreeItem != null) {
                runDefaultActionAfterDoubleClick(selectedTreeItem);
            }
        }
    }

    private void setDragOverStyle(TreeCell<Object> treeCell) {
        treeCell.getStyleClass().add("treecell-drag-over");
    }

    private void fillCellInfosForObject(Object value, TreeCell<Object> treecell, TreeItem<Object> treeItem) {
        if (value == null) {
            GseUtil.setWaitingText(treecell);
        } else {
            setsForObjects(value, treecell, treeItem);
        }
    }

    private void setsForObjects(Object value, TreeCell<Object> treeCell, TreeItem<Object> treeItem) {
        if (value instanceof String) {
            treeCell.setText((String) value);
            treeCell.setGraphic(treeItem.getGraphic());
            treeCell.setTextFill(Color.BLACK);
            treeCell.setOpacity(1);
        } else if (value instanceof ProjectNode) {
            ProjectNode node = (ProjectNode) value;
            treeCell.setText(node.getName());
            treeCell.setGraphic(treeItem.getGraphic());
            treeCell.setTextFill(Color.BLACK);
            treeCell.setOpacity(node instanceof UnknownProjectFile ? 0.5 : 1);
        } else {
            throw new AssertionError("Unexpected type for value: " + value.getClass().getName());
        }
    }

    private boolean isSourceAncestorOf(TreeItem<Object> targetTreeItem) {
        TreeItem treeItemParent = targetTreeItem.getParent();
        while (treeItemParent != null) {
            if (dragAndDropMove.getSourceTreeItem() == treeItemParent) {
                return true;
            } else {
                treeItemParent = treeItemParent.getParent();
            }
        }
        return false;
    }

    private boolean isChildOf(TreeItem<Object> targetTreeItem) {
        return targetTreeItem == dragAndDropMove.getSourceTreeItem().getParent();
    }

    private boolean isMovable(Object item, TreeItem<Object> targetTreeItem) {
        return dragAndDropMove != null && item != dragAndDropMove.getSource() && !isSourceAncestorOf(targetTreeItem) && !isChildOf(targetTreeItem) && !areSourceAndTargetProjectFileSiblings(item);
    }

    private boolean areSourceAndTargetProjectFileSiblings(Object targetItem) {
        Object sourceItem = dragAndDropMove.getSource();
        Optional<ProjectFolder> sourceParent = ((ProjectNode) sourceItem).getParent();
        Optional<ProjectFolder> targetParent = ((ProjectNode) targetItem).getParent();
        if (sourceParent.isPresent() && targetParent.isPresent()) {
            return sourceItem instanceof ProjectFile && targetItem instanceof ProjectFile && sourceParent.get().getId().equals(targetParent.get().getId());
        } else {
            return false;
        }
    }

    private void dragOverEvent(DragEvent event, Object item, TreeItem<Object> treeItem, TreeCell<Object> treeCell) {
        if (item instanceof ProjectNode && isMovable(item, treeItem)) {
            boolean nameExists = item instanceof ProjectFolder ? dragNodeNameAlreadyExists((ProjectFolder) treeItem.getValue()) : dragNodeNameAlreadyExists((ProjectFolder) treeItem.getParent().getValue());
            if (!nameExists) {
                setDragOverStyle(treeCell);
            }
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    private void dragDetectedEvent(Object value, TreeItem<Object> treeItem, MouseEvent event) {
        dragAndDropMove = new DragAndDropMove();
        dragAndDropMove.setSource(value);
        dragAndDropMove.setSourceTreeItem(treeItem);

        if (value instanceof ProjectNode && treeItem != treeView.getRoot()) {
            Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cb = new ClipboardContent();
            cb.putString(((ProjectNode) value).getName());
            db.setContent(cb);
            event.consume();
        }
    }

    private void dragDroppedEvent(Object value, TreeItem<Object> treeItem, DragEvent event, ProjectNode projectNode) {
        if (value != dragAndDropMove.getSource()) {
            success = false;
            if (value instanceof ProjectFolder) {
                ProjectFolder projectFolder = (ProjectFolder) projectNode;
                acceptTransferDrag(projectFolder, success);
                refresh(treeItem);
            } else if (value instanceof ProjectFile) {
                ProjectFile projectFile = (ProjectFile) projectNode;
                projectFile.getParent().ifPresent(projectFolder -> acceptTransferDrag(projectFile.getParent().get(), success));
                refresh(treeItem.getParent());
            }
            event.setDropCompleted(success);
            refresh(dragAndDropMove.getSourceTreeItem().getParent());
            treeView.getSelectionModel().clearSelection();
            event.consume();
        }
    }

    private boolean dragNodeNameAlreadyExists(ProjectFolder projectFolder) {
        return projectFolder.getChildren().stream().anyMatch(projectNode -> projectNode.getName().equals(((ProjectNode) dragAndDropMove.getSource()).getName()));
    }

    private void acceptTransferDrag(ProjectFolder projectFolder, boolean s) {
        success = s;
        if (dragNodeNameAlreadyExists(projectFolder)) {
            GseAlerts.showDraggingError();
        } else {
            ProjectNode monfichier = (ProjectNode) dragAndDropMove.getSource();
            monfichier.moveTo(projectFolder);
            success = true;
        }
    }

    private static String createProjectTooltip(Project project) {
        String result = project.getName() + " (" + project.getPath().toString() + ")";
        if (project.getDescription().isEmpty()) {
            return result;
        }
        return result + "\n" +
                "description: " + project.getDescription();
    }

    private final CreationTaskList tasks = new CreationTaskList();

    public ProjectPane(Scene scene, Project project, GseContext context) {
        this.project = Objects.requireNonNull(project);
        this.context = Objects.requireNonNull(context);

        taskItems = new TaskItemList(project, context);
        taskMonitorPane = new TaskMonitorPane(taskItems);

        treeView = new TreeView<>();
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().getSelectedItems().addListener(this::treeViewChangeListener);
        treeView.setCellFactory(this::treeViewCellFactory);
        treeView.setOnMouseClicked(this::treeViewMouseClickHandler);

        DetachableTabPane ctrlTabPane1 = new DetachableTabPane();
        DetachableTabPane ctrlTabPane2 = new DetachableTabPane();
        ctrlTabPane1.setScope("Control");
        ctrlTabPane2.setScope("Control");
        Tab projectTab = new Tab(RESOURCE_BUNDLE.getString("Data"), treeView);
        projectTab.setClosable(false);
        Tab taskTab = new Tab(RESOURCE_BUNDLE.getString("Tasks"), taskMonitorPane);
        taskTab.setClosable(false);
        ctrlTabPane1.getTabs().add(projectTab);
        ctrlTabPane2.getTabs().add(taskTab);
        SplitPane ctrlSplitPane = new SplitPane(ctrlTabPane1, ctrlTabPane2);
        ctrlSplitPane.setOrientation(Orientation.VERTICAL);
        ctrlSplitPane.setDividerPositions(0.7);

        DetachableTabPane viewTabPane = new DetachableTabPane();
        viewTabPane.setScope("View");
        // register accelerators in the new scene
        viewTabPane.setSceneFactory(tabPane -> {
            FloatingScene floatingScene = new FloatingScene(tabPane, 400, 400);
            GseUtil.registerAccelerators(floatingScene);
            return floatingScene;
        });
        // same title and icon for detached windows
        viewTabPane.setStageOwnerFactory(stage -> {
            stage.setTitle(((Stage) scene.getWindow()).getTitle());
            stage.getIcons().addAll(((Stage) scene.getWindow()).getIcons());
            return scene.getWindow();
        });
        // !!! wrap detachable tab pane in a stack pane to avoid spurious resizing (tiwulfx issue?)
        viewPane = new StackPane(viewTabPane);
        StackPane ctrlPane = new StackPane(ctrlSplitPane);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(ctrlPane, viewPane);
        splitPane.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(ctrlPane, Boolean.FALSE);
        setText(project.getName());
        setTooltip(new Tooltip(createProjectTooltip(project)));
        setContent(splitPane);

        createRootFolderTreeItem(project);

        getContent().setOnKeyPressed((KeyEvent ke) -> {
            if (saveKeyCombination.match(ke)) {
                findDetachableTabPanes().stream()
                        .flatMap(tabPane -> tabPane.getTabs().stream())
                        .map(tab -> ((MyTab) tab).getViewer())
                        .forEach(fileViewer -> {
                            if (fileViewer instanceof Savable && !((Savable) fileViewer).savedProperty().get()) {
                                ((Savable) fileViewer).save();
                            }
                        });
            }
        });
    }

    public Project getProject() {
        return project;
    }

    private static void getTabName(TreeItem<Object> treeItem, StringBuilder builder) {
        if (treeItem != null) {
            getTabName(treeItem.getParent(), builder);
            if (treeItem.getParent() != null) {
                builder.append(treeItem.getValue().toString());
                if (!treeItem.getChildren().isEmpty()) {
                    builder.append("/");
                }
            }
        }
    }

    private static String getTabName(TreeItem<Object> treeItem) {
        StringBuilder builder = new StringBuilder();
        getTabName(treeItem, builder);
        return builder.toString();
    }

    // afs tree view

    private void refresh(TreeItem<Object> item) {
        if (item.getValue() instanceof ProjectFolder) {
            item.setExpanded(false);
            item.setExpanded(true);
        }
    }

    private TreeItem<Object> createNodeTreeItem(ProjectNode node) {
        return node.isFolder() ? createFolderTreeItem((ProjectFolder) node)
                : createFileTreeItem((ProjectFile) node);
    }

    private void createRootFolderTreeItem(Project project) {
        treeView.setRoot(createWaitingTreeItem());
        GseUtil.execute(context.getExecutor(), () -> {
            TreeItem<Object> root = createFolderTreeItem(project.getRootFolder());
            Platform.runLater(() -> {
                treeView.setRoot(root);
                root.setExpanded(true);
            });
        });
    }

    private TreeItem<Object> createFolderTreeItem(ProjectFolder folder) {
        TreeItem<Object> item = new TreeItem<>(folder, NodeGraphics.getGraphic(folder));
        item.setExpanded(false);
        item.expandedProperty().addListener((observable, oldValue, newValue) -> {
            folder.removeAllListeners();
            if (Boolean.TRUE.equals(newValue)) {

                GseUtil.execute(context.getExecutor(), () -> {
                    List<ProjectNode> childNodes = folder.getChildren();

                    List<TreeItem<Object>> childItems = childNodes.stream()
                            .map(ProjectPane.this::createNodeTreeItem)
                            .collect(Collectors.toList());

                    Platform.runLater(() -> {
                        if (item.isExpanded()) {
                            childItems.addAll(tasks.getTaskPreviewNames(folder).stream()
                                    .map(ProjectPane::createTaskTreeItem)
                                    .collect(Collectors.toList()));

                            List<TreeItem<Object>> sortedChildItems = childItems.stream()
                                    .sorted(Comparator.comparing(childItem -> childItem.getValue().toString()))
                                    .collect(Collectors.toList());

                            item.getChildren().setAll(sortedChildItems);
                        }
                    });
                });
            } else {
                // check it is not already a waiting item
                if (item.getChildren().size() != 1 || item.getChildren().get(0).getValue() != null) {
                    item.getChildren().setAll(Collections.singleton(createWaitingTreeItem()));
                }
            }
        });
        item.getChildren().add(createWaitingTreeItem());
        return item;
    }

    private TreeItem<Object> createFileTreeItem(ProjectFile file) {
        return new TreeItem<>(file, NodeGraphics.getGraphic(file));
    }

    private static TreeItem<Object> createWaitingTreeItem() {
        return new TreeItem<>();
    }

    private static TreeItem<Object> createTaskTreeItem(String taskPreviewName) {
        return new TreeItem<>(taskPreviewName, new ImageView(IconCache.INSTANCE.get(ProjectPane.class, "busy16x16")));
    }

    // extension search

    private static List<ProjectFileCreatorExtension> findCreatorExtension(Class<? extends ProjectFile> type) {
        return CREATOR_EXTENSION_LOADER.getServices().stream()
                .filter(extension -> extension.getProjectFileType().isAssignableFrom(type))
                .collect(Collectors.toList());
    }

    private static List<ProjectFileEditorExtension> findEditorExtensions(ProjectFile file) {
        return EDITOR_EXTENSION_LOADER.getServices().stream()
                .filter(extension -> extension.getProjectFileType().isAssignableFrom(file.getClass())
                        && (extension.getAdditionalType() == null || extension.getAdditionalType().isAssignableFrom(file.getClass())))
                .collect(Collectors.toList());
    }

    private static List<ProjectFileViewerExtension> findViewerExtensions(ProjectFile file) {
        return VIEWER_EXTENSION_LOADER.getServices().stream()
                .filter(extension -> extension.getProjectFileType().isAssignableFrom(file.getClass())
                        && (extension.getAdditionalType() == null || extension.getAdditionalType().isAssignableFrom(file.getClass())))
                .collect(Collectors.toList());
    }

    private static List<ProjectFileExecutionTaskExtension> findExecutionTaskExtensions(ProjectFile file) {
        return EXECUTION_TASK_EXTENSION_LOADER.getServices().stream()
                .filter(extension -> extension.getProjectFileType().isAssignableFrom(file.getClass())
                        && (extension.getAdditionalType() == null || extension.getAdditionalType().isAssignableFrom(file.getClass())))
                .collect(Collectors.toList());
    }

    // contextual menu

    private MenuItem createDeleteProjectNodeItem(List<? extends TreeItem<Object>> selectedTreeItems) {
        MenuItem deleteMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Delete"), Glyph.createAwesomeFont('\uf1f8').size("1.1em"));
        deleteMenuItem.setOnAction(event -> deleteNodesAlert(selectedTreeItems));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        return deleteMenuItem;
    }

    private void deleteNodesAlert(List<? extends TreeItem<Object>> selectedTreeItems) {
        GseAlerts.deleteNodesAlert(selectedTreeItems).showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                List<TreeItem<Object>> parentTreeItems = new ArrayList<>();
                for (TreeItem<Object> selectedTreeItem : selectedTreeItems) {
                    ProjectNode node = (ProjectNode) selectedTreeItem.getValue();

                    // close views on this node before deleting the node
                    closeViews(node.getId());

                    node.delete();

                    parentTreeItems.add(selectedTreeItem.getParent());
                }
                for (TreeItem<Object> parentTreeItem : parentTreeItems) {
                    refresh(parentTreeItem);
                }
            }
        });
    }

    private MenuItem createRenameProjectNodeItem(TreeItem selectedTreeItem) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("Rename"), Glyph.createAwesomeFont('\uf120').size("1.1em"));
        menuItem.setOnAction(event -> renameProjectNode(selectedTreeItem));
        return menuItem;
    }

    private void listProjectNodeTreeItems(TreeItem<Object> treeItem, Map<String, TreeItem<Object>> projectNodeTreeItems) {
        if (treeItem.getValue() instanceof ProjectNode) {
            projectNodeTreeItems.put(((ProjectNode) treeItem.getValue()).getId(), treeItem);
        }
        for (TreeItem<Object> childTreeItem : treeItem.getChildren()) {
            listProjectNodeTreeItems(childTreeItem, projectNodeTreeItems);
        }
    }

    private void renameProjectNode(TreeItem<Object> selectedTreeItem) {
        Optional<String> result = RenamePane.showAndWaitDialog(getContent().getScene().getWindow(), (ProjectNode) selectedTreeItem.getValue());
        result.ifPresent(newName -> {
            if (selectedTreeItem.getValue() instanceof ProjectNode) {
                ProjectNode selectedProjectNode = (ProjectNode) selectedTreeItem.getValue();
                selectedProjectNode.rename(newName);

                // to force the refresh
                selectedTreeItem.setValue(null);
                selectedTreeItem.setValue(selectedProjectNode);

                // refresh impacted tabs
                Map<String, TreeItem<Object>> treeItemsToRefresh = new HashMap<>();
                listProjectNodeTreeItems(selectedTreeItem, treeItemsToRefresh);

                for (DetachableTabPane tabPane : findDetachableTabPanes()) {
                    for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                        String tabNodeId = ((TabKey) tab.getUserData()).nodeId;
                        TreeItem<Object> treeItem = treeItemsToRefresh.get(tabNodeId);
                        if (treeItem != null) {
                            tab.setText(getTabName(treeItem));
                        }
                    }
                }
            }
        });
    }

    /**
     * Recursively find DetachableTabPane in the node hierarchy
     */
    private List<DetachableTabPane> findDetachableTabPanes() {
        List<DetachableTabPane> detachableTabPanes = new ArrayList<>();
        findDetachableTabPanes(viewPane, detachableTabPanes);
        // also scan for DetachableTabPane in floating windows
        for (Window window : Window.getWindows()) {
            if (window.getScene() instanceof FloatingScene) {
                findDetachableTabPanes(window.getScene().getRoot(), detachableTabPanes);
            }
        }
        return detachableTabPanes;
    }

    private static void findDetachableTabPanes(Node node, List<DetachableTabPane> detachableTabPanes) {
        if (node instanceof DetachableTabPane) {
            detachableTabPanes.add((DetachableTabPane) node);
        } else if (node instanceof SplitPane) {
            for (Node child : ((SplitPane) node).getItems()) {
                findDetachableTabPanes(child, detachableTabPanes);
            }
        } else if (node instanceof Pane) {
            for (Node child : ((Pane) node).getChildren()) {
                findDetachableTabPanes(child, detachableTabPanes);
            }
        }
    }

    private void closeViews() {
        for (DetachableTabPane tabPane : findDetachableTabPanes()) {
            for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                ((MyTab) tab).requestClose();
            }
        }
    }

    private void closeViews(String nodeId) {
        for (DetachableTabPane tabPane : findDetachableTabPanes()) {
            for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                if (((TabKey) tab.getUserData()).nodeId.equals(nodeId)) {
                    ((MyTab) tab).requestClose();
                }
            }
        }
    }

    private void viewFile(ProjectFile file, ProjectFileViewerExtension viewerExtension, String tabName) {
        List<DetachableTabPane> detachableTabPanes = findDetachableTabPanes();

        TabKey tabKey = new TabKey(file.getId(), viewerExtension.getClass());
        // check tab has not already been open
        for (DetachableTabPane tabPane : detachableTabPanes) {
            for (Tab tab : tabPane.getTabs()) {
                if (tabKey.equals(tab.getUserData())) {
                    tab.getTabPane().getSelectionModel().select(tab);
                    return;
                }
            }
        }
        Node graphic = viewerExtension.getMenuGraphic(file);
        ProjectFileViewer viewer = viewerExtension.newViewer(file, getContent().getScene(), context);
        Tab tab = new MyTab(tabName, viewer);
        tab.setOnCloseRequest(event -> {
            if (!viewer.isClosable()) {
                event.consume();
            }
        });
        tab.setOnClosed(event -> viewer.dispose());
        tab.setGraphic(graphic);
        tab.setTooltip(new Tooltip(tabName));
        tab.setUserData(tabKey);
        DetachableTabPane firstTabPane = detachableTabPanes.get(0);
        firstTabPane.getTabs().add(tab);
        firstTabPane.getSelectionModel().select(tab);
        if (viewer instanceof Savable) {
            ((Savable) viewer).savedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    tab.setText(tabName);
                    tab.getStyleClass().remove("tab-text-unsaved");
                } else if (oldValue) {
                    tab.setText(tabName + STAR_NOTIFICATION);
                    tab.getStyleClass().add("tab-text-unsaved");
                }
            });
        }
        viewer.view();
    }

    private MenuItem initMenuItem(ProjectFileMenuConfigurableExtension menuConfigurable, ProjectFile file) {
        Node graphic = menuConfigurable.getMenuGraphic(file);
        MenuItem menuItem = new MenuItem(menuConfigurable.getMenuText(file), graphic);
        menuItem.setDisable(!menuConfigurable.isMenuEnabled(file));
        return menuItem;
    }

    private void executionTaskLaunch(ProjectFileExecutionTaskExtension executionTaskExtension, ProjectFile file) {
        ExecutionTaskConfigurator configurator = executionTaskExtension.createConfigurator(file, getContent().getScene(), context);
        if (configurator != null) {
            Dialog<Boolean> dialog = null;
            try {
                Callback<ButtonType, Boolean> resultConverter = buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE;
                dialog = new GseDialog<>(configurator.getTitle(), configurator.getContent(), getContent().getScene().getWindow(), configurator.configProperty().isNull(), resultConverter);
                dialog.showAndWait().ifPresent(ok -> {
                    if (ok) {
                        GseUtil.execute(context.getExecutor(), () -> executionTaskExtension.execute(file, configurator.configProperty().get()));
                    }
                });
            } finally {
                if (dialog != null) {
                    dialog.close();
                }
                configurator.dispose();
            }
        } else {
            GseUtil.execute(context.getExecutor(), () -> executionTaskExtension.execute(file, null));
        }
    }

    private ContextMenu createFileContextMenu(TreeItem<Object> selectedTreeItem) {
        ProjectFile file = (ProjectFile) selectedTreeItem.getValue();

        // create menu
        Menu menu = new Menu(RESOURCE_BUNDLE.getString("Open"));

        // add editor extensions
        List<ProjectFileEditorExtension> editorExtensions = findEditorExtensions(file);
        for (ProjectFileEditorExtension editorExtension : editorExtensions) {
            MenuItem menuItem = initMenuItem(editorExtension, file);
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> showProjectItemEditorDialog(file, editorExtension, tabName));
            menuItem.setDisable(!editorExtension.isMenuEnabled(file));
            menu.getItems().add(menuItem);
        }

        // add viewer extensions
        List<ProjectFileViewerExtension> viewerExtensions = findViewerExtensions(file);
        for (ProjectFileViewerExtension viewerExtension : viewerExtensions) {
            MenuItem menuItem = initMenuItem(viewerExtension, file);
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> viewFile(file, viewerExtension, tabName));
            menu.getItems().add(menuItem);
        }

        // add task extensions
        List<ProjectFileExecutionTaskExtension> executionTaskExtensions = findExecutionTaskExtensions(file);
        for (ProjectFileExecutionTaskExtension executionTaskExtension : executionTaskExtensions) {
            MenuItem menuItem = initMenuItem(executionTaskExtension, file);
            menuItem.setOnAction(event -> executionTaskLaunch(executionTaskExtension, file));
            menu.getItems().add(menuItem);
        }

        menu.setDisable(editorExtensions.isEmpty() && viewerExtensions.isEmpty() && executionTaskExtensions.isEmpty());

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(menu);
        contextMenu.getItems().add(createDeleteProjectNodeItem(Collections.singletonList(selectedTreeItem)));
        contextMenu.getItems().add(createRenameProjectNodeItem(selectedTreeItem));
        return contextMenu;
    }

    private MenuItem createCreateFolderItem(TreeItem<Object> selectedTreeItem, ProjectFolder folder) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("CreateFolder") + "...");
        menuItem.setOnAction((ActionEvent event) ->
                NewFolderPane.showAndWaitDialog(getContent().getScene().getWindow(), folder).ifPresent(newFolder -> {
                    refresh(selectedTreeItem);
                    selectedTreeItem.setExpanded(true);
                })
        );
        return menuItem;
    }

    private ContextMenu createMultipleContextMenu(List<? extends TreeItem<Object>> selectedTreeItems) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(createDeleteProjectNodeItem(selectedTreeItems));
        return contextMenu;
    }

    private void showProjectItemCreatorDialog(TreeItem<Object> selectedTreeItem,
                                              ProjectFileCreatorExtension creatorExtension) {
        ProjectFolder folder = (ProjectFolder) selectedTreeItem.getValue();
        ProjectFileCreator creator = creatorExtension.newCreator(folder, getContent().getScene(), context);
        Dialog<Boolean> dialog = createProjectItemDialog(creator.getTitle(), creator.okProperty(), creator.getContent());
        try {
            dialog.showAndWait().filter(result -> result).ifPresent(result -> {
                ProjectCreationTask task = creator.createTask();

                tasks.add(folder, task.getNamePreview());
                refresh(selectedTreeItem);

                GseUtil.execute(context.getExecutor(), () -> {
                    try {
                        task.run();
                    } finally {
                        tasks.remove(folder, task.getNamePreview());
                        Platform.runLater(() -> refresh(selectedTreeItem));
                    }
                });
            });
        } finally {
            dialog.close();
            creator.dispose();
        }
    }

    private void showProjectItemEditorDialog(ProjectFile file, ProjectFileEditorExtension editorExtension, String tabName) {

        ProjectFileEditor editor = editorExtension.newEditor(file, getContent().getScene(), context);
        Dialog<Boolean> dialog = createProjectItemDialog(tabName, editor.okProperty(), editor.getContent());
        editor.edit();

        try {
            dialog.showAndWait()
                    .filter(result -> result)
                    .ifPresent(result -> editor.saveChanges());
        } finally {
            dialog.close();
            editor.dispose();
        }
    }

    private Dialog<Boolean> createProjectItemDialog(String title, BooleanBinding okProperty, javafx.scene.Node content) {
        Callback<ButtonType, Boolean> resultConverter = buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE;
        return new GseDialog<>(title, content, getContent().getScene().getWindow(), okProperty.not(), resultConverter);
    }

    private ContextMenu createFolderContextMenu(TreeItem<Object> selectedTreeItem) {
        ContextMenu contextMenu = new ContextMenu();
        List<MenuItem> items = new ArrayList<>();
        ProjectFolder folder = (ProjectFolder) selectedTreeItem.getValue();
        items.add(createCreateFolderItem(selectedTreeItem, folder));
        for (Class<? extends ProjectFile> type : project.getFileSystem().getData().getProjectFileClasses()) {
            for (ProjectFileCreatorExtension creatorExtension : findCreatorExtension(type)) {
                if (creatorExtension != null) {
                    MenuItem menuItem = new MenuItem(creatorExtension.getMenuText());
                    menuItem.setOnAction(event -> showProjectItemCreatorDialog(selectedTreeItem, creatorExtension));
                    items.add(menuItem);
                }
            }
        }
        if (selectedTreeItem != treeView.getRoot()) {
            items.add(createDeleteProjectNodeItem(Collections.singletonList(selectedTreeItem)));
            items.add(createRenameProjectNodeItem(selectedTreeItem));
        }
        contextMenu.getItems().addAll(items.stream()
                .sorted(Comparator.comparing(MenuItem::getText))
                .collect(Collectors.toList()));
        return contextMenu;
    }

    public void dispose() {
        taskItems.dispose();
        closeViews();
    }

    public boolean canBeClosed() {
        for (DetachableTabPane tabPane : findDetachableTabPanes()) {
            for (Tab tab : new ArrayList<>(tabPane.getTabs())) {
                if (!((MyTab) tab).getViewer().isClosable()) {
                    return false;
                }
            }
        }
        return true;
    }
}
