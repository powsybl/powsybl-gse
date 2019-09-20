/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.google.common.collect.ImmutableList;
import com.powsybl.afs.AppData;
import com.powsybl.afs.Project;
import com.powsybl.afs.ws.client.utils.UserSession;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.spi.*;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.gse.util.NodeChooser;
import com.powsybl.gse.util.Shortcut;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GsePane extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsePane.class);
    private static final String OPENED_PROJECTS = "openedProjects";
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GsePane");
    private static final ServiceLoaderCache<ProjectFileCreatorExtension> CREATOR_EXTENSION = new ServiceLoaderCache<>(ProjectFileCreatorExtension.class);
    private static final ServiceLoaderCache<ProjectFileExecutionTaskExtension> EXECUTION_TASK_EXTENSION = new ServiceLoaderCache<>(ProjectFileExecutionTaskExtension.class);
    private static final ServiceLoaderCache<ProjectFileEditorExtension> EDITOR_EXTENSION = new ServiceLoaderCache<>(ProjectFileEditorExtension.class);
    private static final ServiceLoaderCache<ProjectFileViewerExtension> VIEWER_EXTENSION = new ServiceLoaderCache<>(ProjectFileViewerExtension.class);

    private final KeyCombination closeKeyCombination = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);

    private final GseContext context;

    private final AppData data;

    private final BrandingConfig brandingConfig = BrandingConfig.find();

    private final BorderPane mainPane;
    private final TabPane tabPane = new TabPane();
    private final Preferences preferences;
    private final Application javaxApplication;

    private final KeyCombination createKeyCombination = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);

    private final KeyCombination openKeyCombination = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);

    public GsePane(GseContext context, AppData data, Application app) {
        this.context = Objects.requireNonNull(context);
        this.data = Objects.requireNonNull(data);
        this.javaxApplication = Objects.requireNonNull(app);
        mainPane = new BorderPane();
        mainPane.setTop(createAppBar());
        tabPane.getStyleClass().add("gse-tab-pane");
        tabPane.getTabs().addListener(this::tabPaneChangeListener);
        mainPane.setCenter(tabPane);

        getChildren().addAll(mainPane);

        preferences = Preferences.userNodeForPackage(getClass());
        setOnKeyPressed(ke -> {
            if (createKeyCombination.match(ke)) {
                createNewProject(context, data);
            } else if (openKeyCombination.match(ke)) {
                openProjectDialog(context, data);
            }
        });

        loadPreferences();
    }

    private void tabPaneChangeListener(ListChangeListener.Change<? extends Tab> c) {
        c.getList().forEach(this::onKeyPressed);
    }

    private void onKeyPressed(Tab tab) {
        tab.getContent().setOnKeyPressed((KeyEvent ke) -> {
            if (closeKeyCombination.match(ke)) {
                tab.getTabPane().getTabs().remove(tab);
            }
        });
    }

    private void openProjectDialog(GseContext context, AppData data) {
        Set<String> openedProjects = new HashSet<>();
        for (Tab tab : tabPane.getTabs()) {
            openedProjects.add(((ProjectPane) tab).getProject().getId());
        }
        Optional<Project> project = NodeChooser.showAndWaitDialog(getScene().getWindow(), data, context, Project.class, openedProjects);
        project.ifPresent(this::openProject);
    }

    private void createNewProject(GseContext context, AppData data) {
        Optional<Project> project = NewProjectPane.showAndWaitDialog(getScene().getWindow(), data, context);
        project.ifPresent(this::addProject);
    }

    private void loadPreferences() {
        GseUtil.execute(context.getExecutor(), () -> {
            try {
                List<String> projectPaths = Arrays.asList(preferences.get(OPENED_PROJECTS, "").split(","));
                if (!projectPaths.isEmpty()) {
                    projectPaths.stream()
                            .map(data::getNode)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(Project.class::cast)
                            .filter(project -> !isProjectOpen(project))
                            .forEach(project -> Platform.runLater(() -> addProject(project)));
                }
            } catch (Throwable t) {
                LOGGER.error(t.toString(), t);
            }
        });
    }

    private void savePreferences() {
        List<String> openedProjects = new ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            openedProjects.add(((ProjectPane) tab).getProject().getPath().toString());
        }
        preferences.put(OPENED_PROJECTS, openedProjects.stream().collect(Collectors.joining(",")));
    }

    private void addProject(Project project) {
        ProjectPane projectPane = new ProjectPane(getScene(), project, context);
        tabPane.getTabs().add(projectPane);
        tabPane.getSelectionModel().select(projectPane);
    }

    private boolean isProjectOpen(Project project) {
        Objects.requireNonNull(project);
        for (Tab tab : tabPane.getTabs()) {
            ProjectPane projectPane = (ProjectPane) tab;
            if (projectPane.getProject().getId().equals(project.getId())) {
                return true;
            }
        }
        return false;
    }

    private void cleanClosedProjects() {
        Iterator<Tab> it = tabPane.getTabs().iterator();
        while (it.hasNext()) {
            ProjectPane projectPane = (ProjectPane) it.next();
            if (projectPane.getProject().getFileSystem().isClosed()) {
                it.remove();
            }
        }
    }

    private void showAbout() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        Pane content = brandingConfig.getAboutPane();
        if (content == null) {
            throw new GseException("Branding about pane is null");
        }
        popup.getContent().addAll(content);
        popup.show(getScene().getWindow());
    }

    private void showShortcuts() {
        TableView<Shortcut> tableView = new TableView<>();
        TableColumn<Shortcut, String> column1 = new TableColumn<>(RESOURCE_BUNDLE.getString("Action"));
        column1.setCellValueFactory(new PropertyValueFactory<>("action"));
        TableColumn<Shortcut, String> column2 = new TableColumn<>(RESOURCE_BUNDLE.getString("Shortcut"));
        column2.setCellValueFactory(new PropertyValueFactory<>("keycode"));

        tableView.setPrefSize(430, 445);
        tableView.getColumns().add(column1);
        tableView.getColumns().add(column2);

        createShortcuts().forEach(shortcut -> tableView.getItems().add(shortcut));

        VBox vbox = new VBox(tableView);
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().addAll(vbox);
        popup.show(getScene().getWindow());
    }

    private static List<Shortcut> createShortcuts() {
        List<Shortcut> shortcuts = new ArrayList<>();

        //Standard shortcuts
        final List<Shortcut> standardShortcuts = ImmutableList.of(
                new Shortcut(RESOURCE_BUNDLE.getString("CreateProject"), "CTRL + N"),
                new Shortcut(RESOURCE_BUNDLE.getString("OpenProject"), "CTRL + O"),
                new Shortcut(RESOURCE_BUNDLE.getString("Export"), "CTRL + S"),
                new Shortcut(RESOURCE_BUNDLE.getString("Rename"), "F2"),
                new Shortcut(RESOURCE_BUNDLE.getString("Delete"), RESOURCE_BUNDLE.getString("Delete")),
                new Shortcut(RESOURCE_BUNDLE.getString("Edit"), RESOURCE_BUNDLE.getString("Enter")));
        shortcuts.addAll(standardShortcuts);

        //projectfile creation shortcuts
        for (ProjectFileCreatorExtension creatorExtension : CREATOR_EXTENSION.getServices()) {
            shortcuts.add(new Shortcut(creatorExtension.getMenuText(), creatorExtension.getMenuKeycode().getName()));
        }

        // projectfile edition shortcuts
        for (ProjectFileEditorExtension editorExtension : EDITOR_EXTENSION.getServices()) {
            if (editorExtension.getMenuKeyCode() != null) {
                shortcuts.add(new Shortcut(editorExtension.getMenuText(null), editorExtension.getMenuKeyCode().getName()));
            }
        }

        // projectfile execution shortcuts
        for (ProjectFileExecutionTaskExtension taskExtension : EXECUTION_TASK_EXTENSION.getServices()) {
            if (taskExtension.getMenuKeyCode() != null) {
                shortcuts.add(new Shortcut(taskExtension.getMenuText(null), taskExtension.getMenuKeyCode().getName()));
            }
        }

        // projectfile viewer shortcuts
        for (ProjectFileViewerExtension viewerExtension : VIEWER_EXTENSION.getServices()) {
            if (viewerExtension.getMenuKeyCode() != null) {
                shortcuts.add(new Shortcut(viewerExtension.getMenuText(null), viewerExtension.getMenuKeyCode().getName()));
            }
        }

        shortcuts.sort(Comparator.comparing(Shortcut::getAction));
        return shortcuts;
    }

    private void setUserSession(UserSession userSession) {
        data.setTokenProvider(() -> userSession != null ? userSession.getToken() : null);
    }

    private GseAppBar createAppBar() {
        GseAppBar appBar = new GseAppBar(context, brandingConfig);
        if (appBar.getUserSessionPane() != null) {
            appBar.getUserSessionPane().sessionProperty().addListener((observable, oldUserSession, newUserSession) -> {
                setUserSession(newUserSession);
                if (newUserSession != null) {
                    loadPreferences();
                } else {
                    // clean remote projects
                    cleanClosedProjects();
                }
            });
        }
        appBar.getCreateButton().setOnAction(event -> createNewProject(context, data));
        appBar.getOpenButton().setOnAction(event -> openProjectDialog(context, data));

        ContextMenu contextMenu = new ContextMenu();

        brandingConfig.getDocumentation(javaxApplication).ifPresent(p -> {
            MenuItem documentationMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Documentation") + "...");
            documentationMenuItem.setOnAction(event -> p.show());
            contextMenu.getItems().add(documentationMenuItem);
        });

        MenuItem aboutMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("About") + "...");
        aboutMenuItem.setOnAction(event -> showAbout());
        MenuItem shortcutMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("Shortcuts") + "...");
        shortcutMenuItem.setOnAction(event -> showShortcuts());
        contextMenu.getItems().addAll(aboutMenuItem, shortcutMenuItem);

        appBar.getHelpButton().setOnAction(event -> contextMenu.show(appBar.getHelpButton(), Side.BOTTOM, 0, 0));

        return appBar;
    }

    private void openProject(Project project) {
        if (!isProjectOpen(project)) {
            addProject(project);
        } else {
            for (Tab tab : tabPane.getTabs()) {
                ProjectPane projectPane = (ProjectPane) tab;
                if (projectPane.getProject().getId().equals(project.getId())) {
                    tab.getTabPane().getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    public String getTitle() {
        String title = brandingConfig.getTitle();
        Objects.requireNonNull(title, "Branding title is null");
        return title;
    }

    public List<Image> getIcons() {
        return brandingConfig.getIcons();
    }

    public void dispose() {
        savePreferences();
        for (Tab tab : tabPane.getTabs()) {
            ((ProjectPane) tab).dispose();
        }
    }

    public boolean isClosable() {
        for (Tab tab : tabPane.getTabs()) {
            if (!(((ProjectPane) tab).canBeClosed())) {
                return false;
            }
        }
        return true;
    }
}
