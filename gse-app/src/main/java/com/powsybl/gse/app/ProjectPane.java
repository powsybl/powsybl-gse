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
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.copy_paste.afs.CopyManager;
import com.powsybl.gse.copy_paste.afs.CopyService;
import com.powsybl.gse.copy_paste.afs.exceptions.CopyDifferentFileSystemNameException;
import com.powsybl.gse.copy_paste.afs.exceptions.CopyPasteException;
import com.powsybl.gse.spi.*;
import com.powsybl.gse.util.*;
import com.sun.javafx.stage.StageHelper;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final List<ProjectFileExtension> PROJECT_FILE_EXTENSIONS = new ServiceLoaderCache<>(ProjectFileExtension.class).getServices();

    private static final String STAR_NOTIFICATION = " *";
    private final KeyCombination saveKeyCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    private boolean success;
    private DragAndDropMove dragAndDropMove;
    private BooleanProperty copied = new SimpleBooleanProperty(false);
    private Optional<CopyService> copyService;
    private final Project project;
    private final GseContext context;
    private final TreeTableView<Object> treeView;
    private final StackPane viewPane;
    private final TaskItemList taskItems;
    private final TaskMonitorPane taskMonitorPane;
    private final Map<String, ProjectPaneProjectFolderListener> lCache = new HashMap<>();
    private final CopyManager localArchiveManager = CopyManager.getInstance();

    public ProjectPane(Scene scene, Project project, GseContext context) {
        this.project = Objects.requireNonNull(project);
        this.context = Objects.requireNonNull(context);

        taskItems = new TaskItemList(project, context);
        taskMonitorPane = new TaskMonitorPane(taskItems);
        copyService = initCopyService();
        treeView = createProjectTreeview();

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
        splitPane.setDividerPositions(0.33);
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

        final Clipboard systemClipboard = Clipboard.getSystemClipboard();
        new com.sun.glass.ui.ClipboardAssistance(com.sun.glass.ui.Clipboard.SYSTEM) {
            @Override
            public void contentChanged() {
                if (systemClipboard != null) {
                    boolean canPaste = CopyManager.getCopyInfo(systemClipboard).map(CopyManager.CopyParams::getProjectNodeType).orElse(false);
                    copied.set(canPaste);
                }
            }
        };
    }

    private Optional<CopyService> initCopyService() {
        try {
            return Optional.of(project.getRootFolder().findService(CopyService.class));
        } catch (AfsException e) {
            LOGGER.warn("Failed to initiate copy service", e);
        }
        return Optional.empty();
    }

    private TreeTableView<Object> createProjectTreeview() {
        TreeTableView<Object> treeTableView = new TreeTableView<>();

        // Main Column name
        TreeTableColumn<Object, Object> nameColumn = new TreeTableColumn<>();
        nameColumn.setPrefWidth(270);
        nameColumn.setCellValueFactory(param -> {
            Object data = null;
            if (param != null && param.getValue() != null) {
                data = param.getValue().getValue();
            }
            return new ReadOnlyObjectWrapper<>(data);
        });
        nameColumn.setCellFactory(new Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>>() {
            @Override
            public TreeTableCell<Object, Object> call(TreeTableColumn<Object, Object> param) {
                return new TreeTableCell<Object, Object>() {

                    private Color getProjectFileColor(ProjectFile projectFile) {
                        if (projectFile.mandatoryDependenciesAreMissing()) {
                            return Color.RED;
                        }
                        return Color.BLACK;
                    }

                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else if (item == null) {
                            GseUtil.setWaitingText(this);
                        } else {
                            if (getTreeTableRow().getTreeItem() == null) {
                                return;
                            }
                            setTextFill(Color.BLACK);
                            if (item instanceof String) {
                                setText((String) item);
                                setOpacity(1);
                            } else if (item instanceof ProjectNode) {
                                ProjectNode node = (ProjectNode) item;
                                if (node instanceof ProjectFile) {
                                    setTextFill(getProjectFileColor((ProjectFile) node));
                                }
                                setText(node.getName());
                                setGraphic(NodeGraphics.getGraphic(item));
                                setOpacity(node instanceof UnknownProjectFile ? 0.5 : 1);
                                setOnDragDetected(event -> dragDetectedEvent(getItem(), getTreeTableRow().getTreeItem(), event));
                                setOnDragOver(event -> dragOverEvent(event, getItem(), getTreeTableRow().getTreeItem(), this));
                                setOnDragDropped(event -> dragDroppedEvent(getItem(), getTreeTableRow().getTreeItem(), event, node));
                                setOnDragExited(event -> getStyleClass().removeAll("treecell-drag-over"));
                            } else {
                                throw new NotImplementedException(String.format("Unkown type %s", item.getClass()));
                            }
                        }
                    }
                };
            }
        });

        // Date column
        TreeTableColumn<Object, ZonedDateTime> dateColumn = new TreeTableColumn<>();
        dateColumn.setPrefWidth(120);
        dateColumn.setCellValueFactory(param -> {
            ZonedDateTime objectModificationDate = null;
            if (param != null && param.getValue() != null && param.getValue().getValue() instanceof ProjectNode) {
                objectModificationDate = ((ProjectNode) param.getValue().getValue()).getModificationDate();
            }
            return new ReadOnlyObjectWrapper<>(objectModificationDate);
        });
        dateColumn.setCellFactory(param -> {
            return new TreeTableCell<Object, ZonedDateTime>() {
                @Override
                protected void updateItem(ZonedDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.format(DateTimeFormatter.ofPattern("d MMM yy, HH:mm")));
                    }
                }
            };
        });
        treeTableView.getColumns().add(nameColumn);
//        treeTableView.getColumns().add(dateColumn);
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.getSelectionModel().getSelectedItems().addListener(this::treeViewSelectionChangeListener);
        treeTableView.setOnMouseClicked(this::treeViewMouseClickHandler);
        treeTableView.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                treeTableView.getSelectionModel().clearSelection();
            }
        });
        return treeTableView;
    }

    private void treeViewSelectionChangeListener(ListChangeListener.Change<? extends TreeItem<Object>> c) {
        if (c.getList().isEmpty()) {
            treeView.setContextMenu(createFolderContextMenu(treeView.getRoot()));
        } else if (c.getList().size() == 1) {
            TreeItem<Object> selectedTreeItem = c.getList().get(0);
            Object value = selectedTreeItem.getValue();
            treeView.setOnKeyPressed((KeyEvent ke) -> {
                if (new KeyCodeCombination(KeyCode.F2).match(ke)) {
                    renameProjectNode(selectedTreeItem);
                } else if (new KeyCodeCombination(KeyCode.ENTER).match(ke)) {
                    runDefaultActionAfterDoubleClick(selectedTreeItem);
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

    private void setDragOverStyle(TreeTableCell treeCell) {
        treeCell.getStyleClass().add("treecell-drag-over");
    }

    private boolean isSourceAncestorOf(TreeItem<Object> targetTreeItem) {
        TreeItem treeItemParent = targetTreeItem.getParent();
        while (treeItemParent != null) {
            if (dragAndDropMove.getSourceTreeItem().contains(treeItemParent)) {
                return true;
            } else {
                treeItemParent = treeItemParent.getParent();
            }
        }
        return false;
    }

    private boolean isChildOf(TreeItem<Object> targetTreeItem) {
        return dragAndDropMove
                .getSourceTreeItem()
                .stream()
                .map(TreeItem::getParent)
                .anyMatch(sourceParent -> targetTreeItem.equals(sourceParent) || (!(targetTreeItem.getValue() instanceof FolderBase) && targetTreeItem.getParent() != null && targetTreeItem.getParent().equals(sourceParent)));
    }

    private boolean isMovable(TreeItem<Object> targetTreeItem) {
        return dragAndDropMove != null && !dragAndDropMove.getSourceTreeItem().contains(targetTreeItem) && !isSourceAncestorOf(targetTreeItem) && !isChildOf(targetTreeItem);
    }

    private void dragOverEvent(DragEvent event, Object item, TreeItem<Object> treeItem, TreeTableCell treeCell) {
        if (item instanceof ProjectNode && isMovable(treeItem)) {
            List<TreeItem<Object>> children = (item instanceof ProjectFolder) ?
                    treeItem.getChildren() :
                    Optional.of(treeItem.getParent()).map(parent -> new ArrayList<>(parent.getChildren())).orElse(new ArrayList<>());
            List<String> sourceNames = dragAndDropMove.getSourceTreeItem().stream().map(ProjectPane::getName).collect(Collectors.toList());
            boolean nameExists = children.stream().anyMatch(child -> sourceNames.contains(getName(child)));
            if (!nameExists) {
                setDragOverStyle(treeCell);
            }
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    private static String getName(TreeItem treeItem) {
        if (treeItem.getValue() instanceof com.powsybl.afs.AbstractNodeBase) {
            return ((com.powsybl.afs.AbstractNodeBase) treeItem.getValue()).getName();
        } else {
            throw new IllegalStateException("This method should be used in a project pane using afs nodes");
        }
    }

    private void dragDetectedEvent(Object value, TreeItem<Object> treeItem, MouseEvent event) {
        dragAndDropMove = new DragAndDropMove();
        dragAndDropMove.setSourceTreeItem(new ArrayList<>(treeView.getSelectionModel().getSelectedItems()));

        if (value instanceof ProjectNode && treeItem != treeView.getRoot()) {
            Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cb = new ClipboardContent();
            cb.putString(((ProjectNode) value).getName());
            db.setContent(cb);
            event.consume();
        }
    }

    private void dragDroppedEvent(Object value, TreeItem<Object> treeItem, DragEvent event, ProjectNode projectNode) {
        if (dragAndDropMove.getSourceTreeItem() != null && !dragAndDropMove.getSourceTreeItem().contains(treeItem)) {
            success = false;
            TreeItem<Object> targetDir;
            if (value instanceof ProjectFolder) {
                targetDir = treeItem;
            } else if (value instanceof ProjectFile && treeItem.getParent() != null) {
                targetDir = treeItem.getParent();
            } else {
                GseAlerts.showDraggingError();
                return;
            }

            acceptTransferDrag(targetDir, success);
            refresh(targetDir);

            event.setDropCompleted(success);
            Stream
                    .concat(Stream.of(targetDir), dragAndDropMove
                            .getSourceTreeItem()
                            .stream()
                            .map(TreeItem::getParent)
                            .filter(Objects::nonNull))
                    .distinct()
                    .forEach(this::refresh);

            treeView.getSelectionModel().clearSelection();
            event.consume();
        }
    }

    private boolean dragNodeNameAlreadyExists(TreeItem<Object> projectFolder) {
        List<String> sourceNames = dragAndDropMove.getSourceTreeItem().stream().map(ProjectPane::getName).collect(Collectors.toList());
        return projectFolder.getChildren().stream().anyMatch(child -> sourceNames.contains(getName(child)));
    }

    private void acceptTransferDrag(TreeItem<Object> projectFolder, boolean s) {
        success = s;
        if (!(projectFolder.getValue() instanceof ProjectFolder)) {
            GseAlerts.showDraggingError();
        } else if (dragNodeNameAlreadyExists(projectFolder)) {
            GseAlerts.showDraggingError();
        } else {
            dragAndDropMove.getSourceTreeItem().stream().forEach(item -> {
                if (!(item.getValue() instanceof ProjectNode)) {
                    LOGGER.warn("Drag'n'droppping an unknown type of node (not a ProjectNode)! Ignoring action.");
                } else {
                    ((ProjectNode) item.getValue()).moveTo((ProjectFolder) projectFolder.getValue());
                }
            });
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
            createOrUpdateFolderTreeItem((ProjectFolder) item.getValue(), item);
        }
    }

    private TreeItem<Object> createNodeTreeItem(ProjectNode node, TreeItem<Object> prevItem) {
        return node.isFolder() ? createOrUpdateFolderTreeItem((ProjectFolder) node, prevItem)
                : createFileTreeItem((ProjectFile) node);
    }

    private void createRootFolderTreeItem(Project project) {
        treeView.setRoot(createWaitingTreeItem());
        GseUtil.execute(context.getExecutor(), () -> {
            TreeItem<Object> root = createOrUpdateFolderTreeItem(project.getRootFolder(), null);
            Platform.runLater(() -> {
                treeView.setRoot(root);
                root.setExpanded(true);
                treeView.getRoot().expandedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        refresh(treeView.getRoot());
                    }
                });
            });
        });
    }

    private void updateFolderListener(ProjectFolder folder, TreeItem folderItem) {
        Optional.ofNullable(lCache.getOrDefault(folder.getId(), null)).ifPresent(prevListener -> {
            folder.removeListener(prevListener);
            lCache.remove(folder.getId());
        });
        ProjectPaneProjectFolderListener l = new ProjectPaneProjectFolderListener(folderItem);
        folder.addListener(l);
        lCache.put(folder.getId(), l);
    }

    private TreeItem<Object> createOrUpdateFolderTreeItem(ProjectFolder folder, TreeItem<Object> item) {
        TreeItem<Object> folderItem = item != null ? item : new TreeItem<>(folder);
        if (item == null) {
            folderItem.getChildren().add(createWaitingTreeItem());
        }

        updateFolderListener(folder, folderItem);

        Platform.runLater(() -> {
            List<ProjectNode> childNodes = folder.getChildren();
            List<TreeItem<Object>> childItems = childNodes.stream()
                    .map(child -> new Pair<>(child, findTreeItemFromValue(child, folderItem)))
                    .map(childAndTreeItem -> createNodeTreeItem(childAndTreeItem.getKey(), childAndTreeItem.getValue().orElse(null)))
                    .collect(Collectors.toList());

            childItems.addAll(tasks.getTaskPreviewNames(folder).stream()
                    .map(ProjectPane::createTaskTreeItem)
                    .collect(Collectors.toList()));

            List<TreeItem<Object>> sortedChildItems = childItems.stream()
                    .sorted(Comparator.comparing(childItem -> childItem.getValue().toString()))
                    .collect(Collectors.toList());

            folderItem.getChildren().setAll(sortedChildItems);
        });

        return folderItem;
    }

    private TreeItem<Object> createFileTreeItem(ProjectFile file) {
        return new TreeItem<>(file);
    }

    private static TreeItem<Object> createWaitingTreeItem() {
        return new TreeItem<>();
    }

    private static TreeItem<Object> createTaskTreeItem(String taskPreviewName) {
        return new TreeItem<>(taskPreviewName, new ImageView(IconCache.INSTANCE.get(ProjectPane.class, "busy16x16")));
    }

    private Optional<TreeItem<Object>> findTreeItemFromValue(Object value, TreeItem<Object> root) {
        return findTreeItemFromValue(value, root, 1);
    }

    private Optional<TreeItem<Object>> findTreeItemFromValue(Object value, TreeItem<Object> root, int depth) {
        if (depth < 0) {
            return Optional.empty();
        }

        if (root.getValue() != null &&
                root.getValue() instanceof AbstractNodeBase &&
                value instanceof AbstractNodeBase &&
                ((AbstractNodeBase) (root.getValue())).getId().equals(((AbstractNodeBase) value).getId())) {
            return Optional.of(root);
        }

        if (root.getChildren() != null && root.getChildren().size() > 0) {
            return root.getChildren()
                    .stream()
                    .map(item -> findTreeItemFromValue(value, item, depth - 1))
                    .filter(Optional::isPresent)
                    .findFirst()
                    .orElse(Optional.empty());
        }

        return Optional.empty();
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
        MenuItem deleteMenuItem = GseMenuItem.createDeleteMenuItem();
        deleteMenuItem.setOnAction(event -> deleteNodesAlert(selectedTreeItems));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        List<TreeItem<Object>> selectedItems = new ArrayList<>(selectedTreeItems);
        deleteMenuItem.setDisable(ancestorsExistIn(selectedItems) || selectedItems.contains(treeView.getRoot()));
        return deleteMenuItem;
    }

    private void deleteNodesAlert(List<? extends TreeItem<Object>> selectedTreeItems) {
        List<? extends TreeItem> selection = new ArrayList<>(selectedTreeItems);
        GseAlerts.deleteNodesAlert(selection).showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                selection.stream()
                        .peek(selectedTreeItem -> {
                            ProjectNode node = (ProjectNode) selectedTreeItem.getValue();

                            // close views on this node before deleting the node
                            closeViews(node.getId());

                            node.delete();
                        })
                        .collect(Collectors.groupingBy(TreeItem::getParent))
                        .forEach((parent, items) -> parent.getChildren().removeAll(items));
            }
        });
    }

    private MenuItem createArchiveMenuItem(TreeItem<Object> selectedTreeItem) {
        MenuItem archiveMenuItem = GseMenuItem.createArchiveMenuItem();
        archiveMenuItem.setDisable(!(selectedTreeItem.getValue() instanceof AbstractNodeBase));
        archiveMenuItem.setOnAction(event -> archive((AbstractNodeBase) selectedTreeItem.getValue()));
        return archiveMenuItem;
    }

    private MenuItem createUnarchiveMenuItem(TreeItem<Object> selectedTreeItem) {
        MenuItem unarchiveMenuItem = GseMenuItem.createUnarchiveMenuItem();
        unarchiveMenuItem.setOnAction(event -> unarchiveItems(selectedTreeItem));
        return unarchiveMenuItem;
    }

    private void archive(AbstractNodeBase item) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        java.io.File selectedDirectory = directoryChooser.showDialog(getContent().getScene().getWindow());
        if (selectedDirectory != null) {
            context.getExecutor().execute(() -> {
                TaskMonitor.Task task = project.getFileSystem().getTaskMonitor().startTask(String.format(RESOURCE_BUNDLE.getString("ArchiveTask"), selectedDirectory.getName()), project);
                try {
                    localArchiveManager.copy(Collections.singletonList(item), selectedDirectory);
                } catch (CopyPasteException e) {
                    GseAlerts.showDialogError(e.getMessage());
                } finally {
                    project.getFileSystem().getTaskMonitor().stopTask(task.getId());
                }
            });
        }
    }

    private void unarchiveItems(TreeItem<Object> folderItem) {
        Object folder = folderItem.getValue();
        if (!(folder instanceof ProjectFolder)) {
            throw new IllegalStateException("Can't unarchive item if target is not a project folder!");
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        java.io.File selectedDirectory = directoryChooser.showDialog(getContent().getScene().getWindow());
        if (selectedDirectory != null) {
            ProjectFolder targetFolder = (ProjectFolder) folder;
            context.getExecutor().execute(() -> {
                TaskMonitor.Task task = project.getFileSystem().getTaskMonitor().startTask(String.format(RESOURCE_BUNDLE.getString("UnarchiveTask"), selectedDirectory.getName()), project);
                try {
                    ((ProjectFolder) folder).unarchive(selectedDirectory.toPath());
                    Platform.runLater(() -> refresh(folderItem));
                } catch (Exception e) {
                    Platform.runLater(() -> GseAlerts.showDialogError(e.getMessage()));
                } finally {
                    project.getFileSystem().getTaskMonitor().stopTask(task.getId());
                }
            });
        }
    }

    private MenuItem createCopyProjectNodeItem(List<? extends TreeItem<Object>> selectedTreeItems) {
        MenuItem copyMenuItem = GseMenuItem.createCopyMenuItem();
        List<TreeItem<Object>> selectedItems = new ArrayList<>(selectedTreeItems);
        copyMenuItem.setDisable(ancestorsExistIn(selectedItems) || selectedItems.contains(treeView.getRoot()));
        copyMenuItem.setOnAction(event -> copy(selectedTreeItems));
        return copyMenuItem;
    }

    private MenuItem createPasteProjectNodeItem(TreeItem<Object> selectedTreeItem) {
        MenuItem pasteMenuItem = GseMenuItem.createPasteMenuItem();
        pasteMenuItem.disableProperty().bind(copied.not());
        pasteMenuItem.setOnAction(event -> paste(selectedTreeItem));
        return pasteMenuItem;
    }

    private void copy(List<? extends TreeItem<Object>> selectedTreeItems) {
        if (copyService.isPresent()) {
            List<ProjectNode> projectNodes = selectedTreeItems.stream()
                    .map(item -> (ProjectNode) item.getValue())
                    .collect(Collectors.toList());
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(CopyManager.copyParameters(projectNodes).toString());
            clipboard.setContent(content);
        } else {
            throw new AfsException("copy service not found");
        }
    }

    private void paste(TreeItem<Object> selectedTreeItem) {
        Optional<CopyManager.CopyParams> copyInfo = getCopyInfo();
        context.getExecutor().execute(() -> {
            if (copyService.isPresent()) {
                CopyService cpService = copyService.get();
                copyInfo.ifPresent(cpInfo -> {
                    if (!cpInfo.getFileSystem().equals(project.getFileSystem().getName())) {
                        Platform.runLater(() -> GseAlerts.showDialogError(CopyDifferentFileSystemNameException.MESSAGE));
                        return;
                    }

                    List<CopyManager.CopyParams.NodeInfo> nodesInfos = cpInfo.getNodeInfos();
                    String fileSystemName = cpInfo.getFileSystem();
                    ProjectFolder projectFolder = (ProjectFolder) selectedTreeItem.getValue();
                    String nodeNames = nodesInfos.stream().map(CopyManager.CopyParams.NodeInfo::getName).collect(Collectors.joining(", "));
                    TaskMonitor.Task task = projectFolder.getFileSystem().getTaskMonitor().startTask(String.format(RESOURCE_BUNDLE.getString("CopyPasteTask"), nodeNames), projectFolder.getProject());

                    AtomicReference<CopyPasteException> error = new AtomicReference<>();
                    List<AbstractNodeBase> projectNodes = nodesInfos
                            .stream()
                            .map(CopyManager.CopyParams.NodeInfo::getId)
                            .map(nodeId -> {
                                try {
                                    return fetchProjectNodeWithId(nodeId);
                                } catch (CopyPasteException e) {
                                    LOGGER.error("Failed to fetch project node for node id {}", nodeId);
                                    error.set(e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    if (projectNodes.size() != nodesInfos.size()) {
                        project.getFileSystem().getTaskMonitor().stopTask(task.getId());
                        Platform.runLater(() -> GseAlerts.showDialogCopyError(error.get()));
                        return;
                    }

                    try {
                        project.getFileSystem().getTaskMonitor().updateTaskMessage(task.getId(), String.format(RESOURCE_BUNDLE.getString("CopyTask"), nodeNames));
                        cpService.copy(project.getFileSystem().getName(), projectNodes);
                        project.getFileSystem().getTaskMonitor().updateTaskMessage(task.getId(), String.format(RESOURCE_BUNDLE.getString("PasteTask"), nodeNames));
                        cpService.paste(fileSystemName, nodesInfos.stream().map(CopyManager.CopyParams.NodeInfo::getId).collect(Collectors.toList()), projectFolder);
                        Platform.runLater(() -> {
                            GseAlerts.showPasteCompleteInfo(nodeNames, projectFolder.getName());
                        });
                    } catch (CopyPasteException e) {
                        LOGGER.error("Failed to copy nodes {}", projectNodes, e);
                        Platform.runLater(() -> GseAlerts.showDialogCopyError(e));
                    } finally {
                        project.getFileSystem().getTaskMonitor().stopTask(task.getId());
                        Platform.runLater(() -> refresh(selectedTreeItem));
                    }

                });
            }
        });
    }

    private static Optional<CopyManager.CopyParams> getCopyInfo() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        return CopyManager.getCopyInfo(clipboard);
    }

    private boolean ancestorsExistIn(List<? extends TreeItem<Object>> treeItems) {
        for (TreeItem<Object> treeItem : treeItems) {
            if (treeItem != treeView.getRoot() && hasAncestorsIn(treeItem, treeItems)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAncestorsIn(TreeItem<Object> item, List<? extends TreeItem<Object>> pool) {
        TreeItem<Object> parent = item.getParent();
        while (parent != null) {
            if (pool.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private MenuItem createRenameProjectNodeItem(TreeItem selectedTreeItem) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("Rename"), Glyph.createAwesomeFont('\uf120').size("1.1em"));
        menuItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
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

    private static MenuItem initMenuItem(ProjectFileMenuConfigurableExtension menuConfigurable, ProjectFile file) {
        Node graphic = menuConfigurable.getMenuGraphic(file);
        MenuItem menuItem = new MenuItem(menuConfigurable.getMenuText(file), graphic);
        if (menuConfigurable.getMenuKeyCode() != null) {
            menuItem.setAccelerator(menuConfigurable.getMenuKeyCode());
        }
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

        // add viewer extensions
        List<ProjectFileViewerExtension> viewerExtensions = findViewerExtensions(file);
        for (ProjectFileViewerExtension viewerExtension : viewerExtensions) {
            MenuItem menuItem = initMenuItem(viewerExtension, file);
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> viewFile(file, viewerExtension, tabName));
            menu.getItems().add(menuItem);
        }

        // add editor extensions
        List<ProjectFileEditorExtension> editorExtensions = findEditorExtensions(file);
        for (ProjectFileEditorExtension editorExtension : editorExtensions) {
            MenuItem menuItem = initMenuItem(editorExtension, file);
            String tabName = getTabName(selectedTreeItem);
            menuItem.setOnAction(event -> showProjectItemEditorDialog(file, editorExtension, tabName));
            menuItem.setDisable(!editorExtension.isMenuEnabled(file));
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
        contextMenu.getItems().add(createRenameProjectNodeItem(selectedTreeItem));
        if (copyService.isPresent()) {
            contextMenu.getItems().add(createCopyProjectNodeItem(Collections.singletonList(selectedTreeItem)));
        }
        contextMenu.getItems().add(createArchiveMenuItem(selectedTreeItem));
        contextMenu.getItems().add(createDeleteProjectNodeItem(Collections.singletonList(selectedTreeItem)));
        return contextMenu;
    }

    private MenuItem createCreateFolderItem(TreeItem<Object> selectedTreeItem, ProjectFolder folder) {
        MenuItem menuItem = new MenuItem(RESOURCE_BUNDLE.getString("CreateFolder") + "...");
        Glyph createFolderGlyph = Glyph.createAwesomeFont('\uf07b').size("1.3em").color("#FFDB69");
        menuItem.setGraphic(createFolderGlyph);
        menuItem.setOnAction((ActionEvent event) ->
                NewFolderPane.showAndWaitDialog(getContent().getScene().getWindow(), folder).ifPresent(newFolder -> {
                    refresh(selectedTreeItem);
                    selectedTreeItem.setExpanded(true);
                })
        );
        menuItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        return menuItem;
    }

    private ContextMenu createMultipleContextMenu(List<? extends TreeItem<Object>> selectedTreeItems) {
        ContextMenu contextMenu = new ContextMenu();
        if (copyService.isPresent()) {
            contextMenu.getItems().add(createCopyProjectNodeItem(selectedTreeItems));
        }
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
                    menuItem.setGraphic(creatorExtension.getMenuGraphic());
                    menuItem.setAccelerator(creatorExtension.getMenuKeycode());
                    menuItem.setOnAction(event -> showProjectItemCreatorDialog(selectedTreeItem, creatorExtension));
                    items.add(menuItem);
                }
            }
        }
        if (copyService.isPresent()) {
            items.add(createPasteProjectNodeItem(selectedTreeItem));
        }
        if (folder.getChildren().isEmpty()) {
            items.add(createUnarchiveMenuItem(selectedTreeItem));
        }
        if (selectedTreeItem != treeView.getRoot()) {
            items.add(createRenameProjectNodeItem(selectedTreeItem));
            if (copyService.isPresent()) {
                items.add(createCopyProjectNodeItem(Collections.singletonList(selectedTreeItem)));
            }
            items.add(createArchiveMenuItem(selectedTreeItem));
            items.add(createDeleteProjectNodeItem(Collections.singletonList(selectedTreeItem)));
        }
        contextMenu.getItems().addAll(items);
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

    private AbstractNodeBase fetchProjectNodeWithId(String nodeId) throws CopyPasteException {
        try {
            Field storageField = AppFileSystem.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            ListenableAppStorage storage = (ListenableAppStorage) storageField.get(project.getFileSystem());

            NodeInfo projectFileInfo = storage.getNodeInfo(nodeId);
            NodeInfo parentInfo = storage.getParentNode(projectFileInfo.getId()).orElse(null);
            while (parentInfo != null && !Project.PSEUDO_CLASS.equals(parentInfo.getPseudoClass())) {
                parentInfo = storage.getParentNode(parentInfo.getId()).orElse(null);
            }
            if (parentInfo == null) {
                return project.getFileSystem().createNode(projectFileInfo);
            }

            ProjectFileCreationContext context = new ProjectFileCreationContext(projectFileInfo, storage, project);

            if (ProjectFolder.PSEUDO_CLASS.equals(projectFileInfo.getPseudoClass())) {
                return new ProjectFolder(context);
            }

            Optional<ProjectFileExtension> extension = PROJECT_FILE_EXTENSIONS.stream().filter(pfe -> pfe.getProjectFilePseudoClass().equals(projectFileInfo.getPseudoClass())).findFirst();
            return extension.map(ext -> (AbstractNodeBase) ext.createProjectFile(context)).orElseGet(() -> project.getFileSystem().createNode(projectFileInfo));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new CopyPasteException("Unhandled exception while trying to retrieve node storage", e);
        }
    }

    private class ProjectPaneProjectFolderListener implements ProjectFolderListener {
        private final TreeItem ref;

        public ProjectPaneProjectFolderListener(TreeItem ref) {
            this.ref = ref;
        }

        @Override
        public void childAdded(String nodeId) {
            refresh(ref);
        }

        @Override
        public void childRemoved(String nodeId) {
            // To properly update incomplete object highlighting
            // TODO remove this when event bus allow more fine grained event control
            refresh(treeView.getRoot());
        }
    }

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
            onKeyPressed();
            setContextMenu(contextMenu());
        }

        private void onKeyPressed() {
            getContent().setOnKeyPressed((KeyEvent ke) -> {
                if (closeKeyCombination.match(ke)) {
                    closeTab(ke, this);
                    ke.consume();
                } else if (closeAllKeyCombination.match(ke)) {
                    List<MyTab> mytabs = new ArrayList<>(getTabPane().getTabs().stream()
                            .map(tab -> (MyTab) tab)
                            .collect(Collectors.toList()));
                    mytabs.forEach(mytab -> closeTab(ke, mytab));
                    ke.consume();
                }
            });
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
}
