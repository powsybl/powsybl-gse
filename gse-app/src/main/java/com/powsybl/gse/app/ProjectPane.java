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
import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import com.sun.javafx.stage.StageHelper;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ClipboardContent;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ProjectPane extends Tab {

    private class MoveContext {
        private Object source;
        private TreeItem sourceTreeItem;
        private TreeItem sourceparentTreeItem;
    }

   private MoveContext moveContext;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectPane.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ProjectPane");

    private static final ServiceLoaderCache<ProjectFileCreatorExtension> CREATOR_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileCreatorExtension.class);

    private static final ServiceLoaderCache<ProjectFileEditorExtension> EDITOR_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileEditorExtension.class);

    private static final ServiceLoaderCache<ProjectFileViewerExtension> VIEWER_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileViewerExtension.class);

    private static final ServiceLoaderCache<ProjectFileExecutionTaskExtension> EXECUTION_TASK_EXTENSION_LOADER = new ServiceLoaderCache<>(ProjectFileExecutionTaskExtension.class);

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

        public MyTab(String text, Node content) {
            super(text, content);
        }

        public void requestClose() {
            TabPaneBehavior behavior = getBehavior();
            if (behavior.canCloseTab(this)) {
                behavior.closeTab(this);
            }
        }

        private TabPaneBehavior getBehavior() {
            return ((TabPaneSkin) getTabPane().getSkin()).getBehavior();
        }
    }

    private final Project project;

    private final GseContext context;

    private final TreeView<Object> treeView;

    private final StackPane viewPane;

    private final TaskItemList taskItems;

    private final TaskMonitorPane taskMonitorPane;

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

    private final CreationTaskList tasks = new CreationTaskList();

    public ProjectPane(Scene scene, Project project, GseContext context) {
        this.project = Objects.requireNonNull(project);
        this.context = Objects.requireNonNull(context);

        taskItems = new TaskItemList(project, context);
        taskMonitorPane = new TaskMonitorPane(taskItems);

        treeView = new TreeView<>();
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Object>>) c -> {
            if (c.getList().isEmpty()) {
                treeView.setContextMenu(null);
            } else if (c.getList().size() == 1) {
                TreeItem<Object> selectedTreeItem = c.getList().get(0);
                Object value = selectedTreeItem.getValue();
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
        });
        treeView.setCellFactory(item -> new TreeCell<Object>() {
            @Override
            protected void updateItem(Object value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (value == null) {
                        GseUtil.setWaitingText(this);
                    } else {
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

                            setOnDragDetected(event -> {
                                moveContext = new MoveContext();
                                moveContext.source = getItem();
                                moveContext.sourceTreeItem = getTreeItem();
                                moveContext.sourceparentTreeItem = moveContext.sourceTreeItem.getParent();
                                if (value instanceof ProjectFile || value instanceof ProjectFolder) {
                                    Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
                                    ClipboardContent cb = new ClipboardContent();
                                    if (value instanceof ProjectFile) {
                                        cb.putString(((ProjectFile) value).getName());
                                    } else if (value instanceof ProjectFolder) {
                                        cb.putString(((ProjectFolder) value).getName());
                                    }
                                    db.setContent(cb);
                                    event.consume();
                                }
                            });
                            if (node instanceof ProjectFolder) {
                                ProjectFolder projectFolder = (ProjectFolder) node;
                                setOnDragOver(event -> {
                                    int count = 0;
                                    TreeItem treeItemOvered = getTreeItem();
                                    ObservableList<TreeItem<Object>> treeItemOveredChildrens = treeItemOvered.getChildren();
                                    if (treeItemOveredChildrens.size() < 1) {
                                        count = 0;
                                    } else if (treeItemOveredChildrens.size() >= 1) {
                                        for (TreeItem treeItem : treeItemOveredChildrens) {
                                            if (treeItem.getValue() == null) {
                                                break;
                                            } else if (treeItem.getValue().toString().equals(moveContext.source.toString())) {
                                                count++;
                                            }
                                        }
                                    }
                                    if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                                        if (count < 1) {
                                            setTextFill(Color.CHOCOLATE);
                                            setText(getText().toUpperCase());
                                        }

                                        event.acceptTransferModes(TransferMode.ANY);
                                    }
                                    event.consume();
                                });
                                setOnDragDropped(event -> {
                                    int count = 0;
                                    TreeItem treeItemaccepteur = getTreeItem();
                                    boolean success = false;
                                    Dragboard db = event.getDragboard();

                                    if (getItem() == null) {
                                        return;
                                    }
                                    ObservableList<TreeItem<Object>> treeItemaccepteurchildrens = treeItemaccepteur.getChildren();
                                    if (treeItemaccepteurchildrens.size() < 1) {
                                        count = 0;
                                    } else if (treeItemaccepteurchildrens.size() >= 1) {
                                        for (TreeItem treeItem : treeItemaccepteurchildrens) {
                                            if (treeItem.getValue() == null) {
                                                break;
                                            } else if (treeItem.getValue().toString().equals(moveContext.source.toString())) {
                                                count++;
                                            }
                                        }
                                    }
                                    if (count >= 1) {
                                        Alert alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle(RESOURCE_BUNDLE.getString("DragError"));
                                        alert.setHeaderText(RESOURCE_BUNDLE.getString("Error"));
                                        alert.setContentText(RESOURCE_BUNDLE.getString("DragFileExists"));
                                        alert.showAndWait();
                                    } else if (db.hasString() && count < 1) {
                                        if (moveContext.source instanceof ProjectNode) {
                                            ProjectNode monfichier = (ProjectNode) moveContext.source;
                                            monfichier.moveTo(projectFolder);

                                            refresh(moveContext.sourceparentTreeItem);
                                            refresh(treeItemaccepteur);

                                            success = true;
                                        }

                                    } else {
                                    }
                                    event.setDropCompleted(success);
                                    event.consume();
                                });
                                setOnDragExited(event -> {
                                    setTextFill(Color.BLACK);
                                    setText(getText().toLowerCase());
                                });

                            }

                        } else {
                            throw new AssertionError();
                        }
                    }
                }
            }
        });
        treeView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                TreeItem<Object> selectedTreeItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedTreeItem != null) {
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
            }
        });
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
        setTooltip(new Tooltip(project.getName() + ": " + project.getDescription()));
        setContent(splitPane);

        createRootFolderTreeItem(project);
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
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("Delete"), Glyph.createAwesomeFont('\uf1f8').size("1.1em"));
        menuItem.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(RESOURCE_BUNDLE.getString("ConfirmationDialog"));
            String headerText;
            if (selectedTreeItems.size() == 1) {
                ProjectNode node = (ProjectNode) selectedTreeItems.get(0).getValue();
                headerText = String.format(RESOURCE_BUNDLE.getString("FileWillBeDeleted"), node.getName());
            } else if (selectedTreeItems.size() > 1) {
                String names = selectedTreeItems.stream()
                        .map(selectedTreeItem -> selectedTreeItem.getValue().toString())
                        .collect(Collectors.joining(", "));
                headerText = String.format(RESOURCE_BUNDLE.getString("FilesWillBeDeleted"), names);
            } else {
                throw new AssertionError();
            }
            alert.setHeaderText(headerText);
            alert.setContentText(RESOURCE_BUNDLE.getString("DoYouConfirm"));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
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
        return menuItem;
    }

    private MenuItem createRenameProjectNodeItem(TreeItem selectedTreeItem) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("Rename"));
        menuItem.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(RESOURCE_BUNDLE.getString("NewName"));
            dialog.setTitle(RESOURCE_BUNDLE.getString("RenameFolder"));
            dialog.setHeaderText(RESOURCE_BUNDLE.getString("NewName"));
            dialog.setContentText(RESOURCE_BUNDLE.getString("Names"));
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newname -> {
                if (selectedTreeItem.getValue() instanceof ProjectNode) {
                    ProjectNode selectedTreeNode = (ProjectNode) selectedTreeItem.getValue();
                    selectedTreeNode.rename(newname);
                    refresh(selectedTreeItem.getParent());
                }
            });

        });
        return menuItem;
    }

    /**
     * Recursively find DetachableTabPane in the node hierarchy
     */
    private List<DetachableTabPane> findDetachableTabPanes() {
        List<DetachableTabPane> detachableTabPanes = new ArrayList<>();
        findDetachableTabPanes(viewPane, detachableTabPanes);
        // also scan for DetachableTabPane in floating windows
        for (Stage stage : StageHelper.getStages()) {
            if (stage.getScene() instanceof FloatingScene) {
                findDetachableTabPanes(stage.getScene().getRoot(), detachableTabPanes);
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
        Tab tab = new MyTab(tabName, viewer.getContent());
        tab.setOnClosed(event -> viewer.dispose());
        tab.setGraphic(graphic);
        tab.setTooltip(new Tooltip(tabName));
        tab.setUserData(tabKey);
        DetachableTabPane firstTabPane = detachableTabPanes.get(0);
        firstTabPane.getTabs().add(tab);
        firstTabPane.getSelectionModel().select(tab);
        viewer.view();
    }

    private ContextMenu createFileContextMenu(TreeItem<Object> selectedTreeItem) {
        ProjectFile file = (ProjectFile) selectedTreeItem.getValue();

        // create menu
        Menu menu = new Menu(RESOURCE_BUNDLE.getString("Open"));

        // add editor extensions
        List<ProjectFileEditorExtension> editorExtensions = findEditorExtensions(file);
        for (ProjectFileEditorExtension editorExtension : editorExtensions) {
            MenuItem menuItem = new MenuItem(editorExtension.getMenuText(file));
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> showProjectItemEditorDialog(file, editorExtension, tabName));
            menu.getItems().add(menuItem);
        }

        // add viewer extensions
        List<ProjectFileViewerExtension> viewerExtensions = findViewerExtensions(file);
        for (ProjectFileViewerExtension viewerExtension : viewerExtensions) {
            Node graphic = viewerExtension.getMenuGraphic(file);
            MenuItem menuItem = new MenuItem(viewerExtension.getMenuText(file), graphic);
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> viewFile(file, viewerExtension, tabName));
            menuItem.setDisable(!viewerExtension.isMenuEnabled(file));
            menu.getItems().add(menuItem);
        }

        // add task extensions
        List<ProjectFileExecutionTaskExtension> executionTaskExtensions = findExecutionTaskExtensions(file);
        for (ProjectFileExecutionTaskExtension executionTaskExtension : executionTaskExtensions) {
            MenuItem menuItem = new MenuItem(executionTaskExtension.getMenuText() + "...");
            menuItem.setOnAction(event -> {
                ExecutionTaskConfigurator configurator = executionTaskExtension.createConfigurator(file, getContent().getScene(), context);
                if (configurator != null) {
                    Dialog<Boolean> dialog = new Dialog<>();
                    try {
                        dialog.setTitle(configurator.getTitle());
                        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                        Button button = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
                        button.disableProperty().bind(configurator.configProperty().isNull());
                        dialog.getDialogPane().setContent(configurator.getContent());
                        dialog.setResizable(true);
                        dialog.initOwner(getContent().getScene().getWindow());
                        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE);
                        dialog.showAndWait().ifPresent(ok -> {
                            if (ok) {
                                GseUtil.execute(context.getExecutor(), () -> executionTaskExtension.execute(file, configurator.configProperty().get()));
                            }
                        });
                    } finally {
                        dialog.close();
                        configurator.dispose();
                    }
                } else {
                    GseUtil.execute(context.getExecutor(), () -> executionTaskExtension.execute(file, null));
                }
            });
            menu.getItems().add(menuItem);
        }

        menu.setDisable(editorExtensions.isEmpty() && viewerExtensions.isEmpty() && executionTaskExtensions.isEmpty());

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(menu);
        contextMenu.getItems().add(createDeleteProjectNodeItem(Collections.singletonList(selectedTreeItem)));
        return contextMenu;
    }

    private MenuItem createCreateFolderItem(TreeItem<Object> selectedTreeItem, ProjectFolder folder) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("CreateFolder") + "...");
        menuItem.setOnAction((ActionEvent event) -> {
            NewFolderPane.showAndWaitDialog(getContent().getScene().getWindow(), folder).ifPresent(newFolder -> {
                refresh(selectedTreeItem);
                selectedTreeItem.setExpanded(true);
            });
        });
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
            if (dialog.showAndWait().get()) {
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
            }
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
            if (dialog.showAndWait().get()) {
                editor.saveChanges();
            }
        } finally {
            dialog.close();
            editor.dispose();
        }
    }

    private Dialog<Boolean> createProjectItemDialog(String title, BooleanBinding okProperty, javafx.scene.Node content) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button button = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        button.disableProperty().bind(okProperty.not());
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.initOwner(getContent().getScene().getWindow());
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE);
        return dialog;
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
    }
}
