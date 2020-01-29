/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ext.base.AbstractScript;
import com.powsybl.afs.ext.base.ScriptListener;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.spi.AutoCompletionWordsProvider;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.spi.Savable;
import com.powsybl.gse.util.Glyph;
import com.powsybl.gse.util.GseAlerts;
import com.powsybl.gse.util.GseDialog;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.AbstractCodeEditorFactoryService;
import com.powsybl.gse.util.editor.impl.GroovyCodeEditor;
import groovy.lang.GroovyShell;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import org.controlsfx.control.MasterDetailPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ModificationScriptEditor extends BorderPane
        implements ProjectFileViewer, Savable, ScriptListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModificationScriptEditor.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ModificationScript");

    private static final int VALIDATION_INFO_TIMEOUT = 3000;

    private static final double DIVIDER_POSITION = 0.05;

    private static final ServiceLoaderCache<AbstractCodeEditorFactoryService> CODE_EDITOR_FACTORIES = new ServiceLoaderCache<>(AbstractCodeEditorFactoryService.class);

    private static final String GSE_TOOLBAR_BUTTON_STYLE = "gse-toolbar-button";

    private final GseContext context;

    private final ToolBar toolBar;

    private final ToolBar bottomToolBar;

    private final ComboBox<Integer> comboBox;

    private final Label tabSizeLabel;

    private Label caretPositionDisplay;

    private final Button saveButton;

    private final Button validateButton;

    private final Button addVirtualScriptButton;

    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    private final StackPane codeEditorWithProgressIndicator;

    private final MasterDetailPane codeEditorWithIncludesPane;

    private final SplitPane splitPane;

    private AbstractCodeEditor codeEditor;

    private Optional<AbstractCodeEditorFactoryService> preferredCodeEditor;

    private AbstractScript abstractScript;

    private final SimpleBooleanProperty saved = new SimpleBooleanProperty(true);

    private Service<String> scriptUpdateService;

    public ModificationScriptEditor(AbstractScript abstractScript, Scene scene, GseContext context) {
        this.abstractScript = abstractScript;
        this.context = context;

        preferredCodeEditor = CODE_EDITOR_FACTORIES.getServices()
                .stream()
                .findAny();

        codeEditor = getCodeEditor();

        //Adding  autocompletion keywords suggestions depending the context
        List<String> suggestions = new ArrayList<>();
        List<AutoCompletionWordsProvider> completionWordsProviderExtensions = findCompletionWordsProviderExtensions(abstractScript);
        completionWordsProviderExtensions.forEach(extension -> suggestions.addAll(extension.completionKeyWords()));

        codeEditorWithProgressIndicator = new StackPane();
        codeEditorWithIncludesPane = new MasterDetailPane();
        codeEditorWithIncludesPane.setMasterNode(codeEditorWithProgressIndicator);
        codeEditorWithIncludesPane.setDetailNode(createIncludedScriptsPane(false));
        codeEditorWithIncludesPane.setShowDetailNode(true);
        codeEditorWithIncludesPane.setDetailSide(Side.TOP);
        codeEditorWithIncludesPane.setDividerPosition(DIVIDER_POSITION);
        splitPane = new SplitPane(codeEditorWithIncludesPane);
        setUpEditor(codeEditor, suggestions);
        codeEditor.setTabSize(4);

        MasterDetailPane codeWithSyntaxCheckPane = new MasterDetailPane();
        codeWithSyntaxCheckPane.setAnimated(true);
        codeWithSyntaxCheckPane.setDetailSide(Side.BOTTOM);
        codeWithSyntaxCheckPane.setShowDetailNode(false);
        codeWithSyntaxCheckPane.setMasterNode(splitPane);

        Text saveGlyph = Glyph.createAwesomeFont('\uf0c7').size("1.3em");
        saveButton = new Button("", saveGlyph);
        saveButton.getStyleClass().add(GSE_TOOLBAR_BUTTON_STYLE);
        saveButton.disableProperty().bind(saved);
        saveButton.setOnAction(event -> save());

        Image validateImage = new Image(ModificationScriptEditor.class.getResourceAsStream("/icons/spell-check-solid.png"));
        validateButton = new Button("", new ImageView(validateImage));
        validateButton.getStyleClass().add(GSE_TOOLBAR_BUTTON_STYLE);
        validateButton.setOnAction(event -> validateScript(codeWithSyntaxCheckPane));

        Text plusGlyph = Glyph.createAwesomeFont('\uf055').size("1.3em");
        addVirtualScriptButton = new Button("", plusGlyph);
        addVirtualScriptButton.getStyleClass().add(GSE_TOOLBAR_BUTTON_STYLE);
        addVirtualScriptButton.setOnAction(event -> addVirtualScript(scene, context));

        comboBox = new ComboBox(FXCollections.observableArrayList(2, 4, 8));
        comboBox.getSelectionModel().select(1);
        comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> codeEditor.setTabSize(comboBox.getItems().get((int) newvalue)));
        tabSizeLabel = new Label(RESOURCE_BUNDLE.getString("TabSize") + ": ");
        caretPositionDisplay = new Label(codeEditor.currentPosition());

        toolBar = new ToolBar(saveButton, validateButton, addVirtualScriptButton);

        Pane spacer = new Pane();
        bottomToolBar = new ToolBar(tabSizeLabel, comboBox, spacer, caretPositionDisplay);
        bottomToolBar.widthProperty().addListener((observable, oldvalue, newvalue) -> spacer.setPadding(new Insets(0, (double) newvalue - 340, 0, 0)));
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.8);
        setTop(toolBar);
        setBottom(bottomToolBar);
        setCenter(codeWithSyntaxCheckPane);

        // listen to modifications
        abstractScript.addListener(this);
    }

    private void addVirtualScript(Scene scene, GseContext context) {
        VirtualScriptCreator virtualScriptCreator = new VirtualScriptCreator(abstractScript, scene, context);
        Callback<ButtonType, Boolean> resultConverter = buttonType -> buttonType == ButtonType.OK ? Boolean.TRUE : Boolean.FALSE;
        Dialog<Boolean> dialog = new GseDialog<>(virtualScriptCreator.getTitle(), virtualScriptCreator, scene.getWindow(), virtualScriptCreator.okProperty().not(), resultConverter);
        dialog.showAndWait().filter(result -> result).ifPresent(result -> updateIncludePane(virtualScriptCreator::create));
    }

    private void removeVirtualScript(AbstractScript script) {
        Optional<ButtonType> result = GseAlerts.showRemoveConfirmationAlert(script.getName());
        result.filter(type -> type == ButtonType.OK).ifPresent(okButton -> updateIncludePane(() -> abstractScript.removeScript(script.getId())));
    }

    private void setUpEditor(AbstractCodeEditor editor, List<String> completions) {
        String prevContent = codeEditor != null ? codeEditor.getCode() : "";
        codeEditor = editor;
        codeEditor.setCode(prevContent);
        codeEditor.setCompletions(completions);
        codeEditor.caretPositionProperty().addListener((observable, oldValue, newValue) -> caretPositionDisplay.setText(codeEditor.currentPosition()));
        codeEditor.codeProperty().addListener((observable, oldValue, newValue) -> saved.set(false));
        codeEditorWithProgressIndicator.getChildren().clear();
        codeEditorWithProgressIndicator.getChildren().addAll(codeEditor, new Group(progressIndicator));
    }

    private List<AutoCompletionWordsProvider> findCompletionWordsProviderExtensions(AbstractScript script) {
        return GroovyCodeEditor.findAutoCompletionWordProviderExtensions(script.getClass());
    }

    private void validateScript(MasterDetailPane codeWithDetailPane) {
        if (ScriptType.GROOVY.equals(abstractScript.getScriptType())) {
            try {
                GroovyShell groovyShell = new GroovyShell();
                groovyShell.parse(codeEditor.getCode());
                codeWithDetailPane.setDetailNode(createScriptValidationOkNode(() -> Platform.runLater(() -> codeWithDetailPane.setShowDetailNode(false))));
                codeWithDetailPane.resetDividerPosition();
                codeWithDetailPane.setShowDetailNode(true);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> codeWithDetailPane.setShowDetailNode(false));
                    }
                }, VALIDATION_INFO_TIMEOUT);
            } catch (Exception e) {
                codeWithDetailPane.setDetailNode(createScriptValidationKoNode(e, () -> Platform.runLater(() -> codeWithDetailPane.setShowDetailNode(false))));
                codeWithDetailPane.resetDividerPosition();
                codeWithDetailPane.setShowDetailNode(true);
            }
        } else {
            LOGGER.info("No validation process found for script type {}", abstractScript.getScriptType());
        }
    }

    private void updateIncludePane(Runnable runnable) {
        runnable.run();
        Platform.runLater(() -> codeEditorWithIncludesPane.setDetailNode(createIncludedScriptsPane(true)));
    }

    private Node createIncludedScriptsPane(boolean isExpanded) {
        List<AbstractScript> includedScripts = abstractScript.getIncludedScripts();
        List<IncludeScriptPane> includedScriptsPanes = includedScripts.stream()
                .map(this::includedPane)
                .collect(Collectors.toList());

        List<HBox> allIncludesPanes = new ArrayList<>();
        includedScriptsPanes.forEach(includedScriptPane -> {
            Button removeButton = createIncludedButton('\uf056', null, "1.1em", event -> removeVirtualScript(includedScriptPane.getIncludedScript()));
            Button upButton = createIncludedButton('\uf062', "green", "0.9em", event -> {
            });
            Button downButton = createIncludedButton('\uf063', "red", "0.9em", event -> {
            });
            upButton.getStyleClass().add("up-down-button");
            downButton.getStyleClass().add("up-down-button");
            HBox includePaneWithRemoveButton = new HBox(includedScriptPane, removeButton, upButton, downButton);
            allIncludesPanes.add(includePaneWithRemoveButton);
            HBox.setHgrow(includedScriptPane, Priority.ALWAYS);
        });

        VBox includesPanesBox = new VBox();
        includesPanesBox.getChildren().addAll(allIncludesPanes);

        String includeScriptsLabel = includedScripts.size() + " " + RESOURCE_BUNDLE.getString("IncludedScripts");
        TitledPane rootTitledPane = new TitledPane(includeScriptsLabel, includesPanesBox);
        rootTitledPane.setExpanded(isExpanded);
        rootTitledPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                codeEditorWithIncludesPane.setDividerPosition(0.5);
            } else {
                codeEditorWithIncludesPane.setDividerPosition(DIVIDER_POSITION);
            }
        });

        return rootTitledPane;
    }

    private IncludeScriptPane includedPane(AbstractScript script) {
        AbstractCodeEditor includedScriptCodeEditor = getCodeEditor();
        includedScriptCodeEditor.setCode(script.readScript());
        includedScriptCodeEditor.setEditable(false);
        IncludeScriptPane titledPane = new IncludeScriptPane(script.getName(), includedScriptCodeEditor, script);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private Button createIncludedButton(char font, String color, String size, EventHandler<ActionEvent> eventHandler) {
        Glyph glyph = Glyph.createAwesomeFont(font);
        if (size != null) {
            glyph.size(size);
        }
        if (color != null) {
            glyph.color(color);
        }
        Button btn = new Button("", glyph);
        btn.setOnAction(eventHandler);
        return btn;
    }

    private AbstractCodeEditor getCodeEditor() {
        return preferredCodeEditor
                .map(codeEditorFactoryService -> {
                    try {
                        LOGGER.info("Trying to use custom editor {}", codeEditorFactoryService.getEditorClass());
                        return codeEditorFactoryService.build();
                    } catch (Exception e) {
                        LOGGER.error("Failed to instanciate editor {}", codeEditorFactoryService.getEditorClass(), e);
                    }
                    return null;
                })
                .orElse(new GroovyCodeEditor());
    }

    private Node createScriptValidationNode(Text message, int prefHeight, TextAlignment alignment, Color backgroundColor, Runnable closeDetail) {
        AnchorPane root = new AnchorPane();
        Background background = new Background(new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY));
        root.setBackground(background);

        TextFlow result = new TextFlow();
        result.setBackground(background);
        result.getChildren().add(message);
        result.setTextAlignment(alignment);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(result);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(5));
        root.getChildren().add(scrollPane);
        AnchorPane.setLeftAnchor(scrollPane, 0.0);
        AnchorPane.setTopAnchor(scrollPane, 0.0);
        AnchorPane.setRightAnchor(scrollPane, 30.0);
        AnchorPane.setBottomAnchor(scrollPane, 0.0);

        VBox box = new VBox();
        box.setPrefWidth(30.0);
        box.setAlignment(Pos.TOP_CENTER);
        Button closeButton = new Button();
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(event -> closeDetail.run());
        box.getChildren().add(closeButton);
        root.getChildren().add(box);
        AnchorPane.setTopAnchor(box, 0.0);
        AnchorPane.setRightAnchor(box, 0.0);

        root.setPrefHeight(prefHeight);

        return root;
    }

    private Node createScriptValidationOkNode(Runnable closeDetail) {
        Text message = new Text("Ok");
        return createScriptValidationNode(message, 20, TextAlignment.CENTER, Color.web("#53e681"), closeDetail);
    }

    private Node createScriptValidationKoNode(Exception e, Runnable closeDetail) {
        return createScriptValidationNode(new Text(e.getLocalizedMessage()), 110, TextAlignment.LEFT, Color.web("#e85f5f"), closeDetail);
    }

    @Override
    public void save() {
        if (!saved.getValue()) {
            // write script but remove listener before to avoid double update
            abstractScript.removeListener(this);
            abstractScript.writeScript(codeEditor.getCode());
            abstractScript.addListener(this);
            saved.setValue(true);
        }
    }

    @Override
    public Node getContent() {
        return this;
    }

    private void updateScript() {
        scriptUpdateService = GseUtil.createService(new Task<String>() {
            @Override
            protected String call() {
                return abstractScript.readScript();
            }
        }, context.getExecutor());
        progressIndicator.visibleProperty().bind(scriptUpdateService.runningProperty());
        codeEditor.disableProperty().bind(scriptUpdateService.runningProperty());
        scriptUpdateService.setOnSucceeded(event -> {
            String scriptContent = (String) event.getSource().getValue();
            codeEditor.setCode(scriptContent);
            saved.set(true);

        });
        scriptUpdateService.start();
    }

    @Override
    public SimpleBooleanProperty savedProperty() {
        return saved;
    }

    @Override
    public void scriptUpdated() {
        updateScript();
    }

    @Override
    public void view() {
        updateScript();
    }

    @Override
    public void dispose() {
        abstractScript.removeListener(this);
    }

    @Override
    public boolean isClosable() {
        if (!saved.get()) {
            return GseAlerts.showSaveDialog(abstractScript.getName(), this);
        }
        return true;
    }

    private static class IncludeScriptPane extends TitledPane {

        private AbstractScript includedScript;

        IncludeScriptPane(String title, Node content, AbstractScript includedScript) {
            super(title, content);
            this.includedScript = includedScript;
        }

        public AbstractScript getIncludedScript() {
            return includedScript;
        }

    }

}
