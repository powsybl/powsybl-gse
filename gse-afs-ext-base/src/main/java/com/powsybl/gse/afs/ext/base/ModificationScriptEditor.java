/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.common.collect.ImmutableList;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ext.base.ScriptListener;
import com.powsybl.afs.ext.base.StorableScript;
import com.powsybl.gse.spi.AutoCompletionWordsProvider;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.spi.Savable;
import com.powsybl.gse.util.*;
import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.impl.js.AceCodeEditor;
import com.powsybl.gse.util.editor.impl.js.CodeMirrorEditor;
import com.powsybl.gse.util.editor.impl.swing.AlternateCodeEditor;
import com.powsybl.gse.util.editor.impl.javafx.GroovyCodeEditor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ModificationScriptEditor extends BorderPane
        implements ProjectFileViewer, Savable, ScriptListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModificationScriptEditor.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ModificationScript");

    private static final List<String> STANDARD_SUGGESTIONS = ImmutableList.of("as", "assert", "boolean", "break", "breaker", "byte",
            "case", "catch", "char", "class", "continue", "def", "default", "double", "else", "enum",
            "extends", "false", "finally", "float", "for", "generator", "if", "implements", "import", "in",
            "instanceof", "int", "interface", "load", "long", "native", "network", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "substation", "super", "switch", "synchronized", "this",
            "threadsafe", "throw", "throws", "transient", "true", "try", "void", "volatile", "voltageLevel", "while"
    );

    private final GseContext context;

    private final ToolBar toolBar;

    private final ToolBar bottomToolBar;

    private final ComboBox<Integer> comboBox;

    private final Label tabSizeLabel;

    private Label caretPositionDisplay;

    private final Button saveButton;

    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    private final StackPane codeEditorWithProgressIndicator;

    private final SplitPane splitPane;

    private AbstractCodeEditor codeEditor;

    private StorableScript storableScript;

    private final SimpleBooleanProperty saved = new SimpleBooleanProperty(true);

    private Service<String> scriptUpdateService;

    public ModificationScriptEditor(StorableScript storableScript, Scene scene, GseContext context) {
        this.storableScript = storableScript;
        this.context = context;

        //Adding  autocompletion keywords suggestions depending the context
        List<String> suggestions = new ArrayList<>(STANDARD_SUGGESTIONS);
        List<AutoCompletionWordsProvider> completionWordsProviderExtensions = findCompletionWordsProviderExtensions(storableScript);
        completionWordsProviderExtensions.forEach(extension -> suggestions.addAll(extension.completionKeyWords()));

        codeEditorWithProgressIndicator = new StackPane();
        splitPane = new SplitPane(codeEditorWithProgressIndicator);
        setUpEditor(new CodeMirrorEditor(scene, suggestions));
        codeEditor.setTabSize(4);

        Text saveGlyph = Glyph.createAwesomeFont('\uf0c7').size("1.3em");
        saveButton = new Button("", saveGlyph);
        saveButton.getStyleClass().add("gse-toolbar-button");
        saveButton.disableProperty().bind(saved);
        saveButton.setOnAction(event -> save());
        comboBox = new ComboBox(FXCollections.observableArrayList(2, 4, 8));
        comboBox.getSelectionModel().select(1);
        comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> codeEditor.setTabSize(comboBox.getItems().get((int) newvalue)));
        tabSizeLabel = new Label(RESOURCE_BUNDLE.getString("TabSize") + ": ");
        caretPositionDisplay = new Label(codeEditor.currentPosition());

        toolBar = new ToolBar(saveButton);

        ToggleSwitch editorSwitch = new ToggleSwitch();
        editorSwitch.setSelected(true);
        editorSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {
                setUpEditor(newValue ? new AlternateCodeEditor(scene, suggestions) : new GroovyCodeEditor(scene, suggestions));
            }
        });

        Pane spacer = new Pane();
        bottomToolBar = new ToolBar(tabSizeLabel, comboBox, spacer, caretPositionDisplay, editorSwitch);
        bottomToolBar.widthProperty().addListener((observable, oldvalue, newvalue) -> spacer.setPadding(new Insets(0, (double) newvalue - 240, 0, 0)));
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.8);
        setTop(toolBar);
        setBottom(bottomToolBar);
        setCenter(splitPane);

        // listen to modifications
        storableScript.addListener(this);
    }

    private void setUpEditor(AbstractCodeEditor editor) {
        String prevContent = codeEditor != null ? codeEditor.getCode() : "";
        codeEditor = editor;
        codeEditor.setCode(prevContent);
        codeEditor.caretPositionProperty().addListener((observable, oldValue, newValue) -> caretPositionDisplay.setText(codeEditor.currentPosition()));
        codeEditor.codeProperty().addListener((observable, oldValue, newValue) -> saved.set(false));
        codeEditorWithProgressIndicator.getChildren().clear();
        codeEditorWithProgressIndicator.getChildren().addAll(codeEditor, new Group(progressIndicator));
    }

    private List<AutoCompletionWordsProvider> findCompletionWordsProviderExtensions(StorableScript storableScript) {
        return GroovyCodeEditor.findAutoCompletionWordProviderExtensions(storableScript.getClass());
    }

    @Override
    public void save() {
        if (!saved.getValue()) {
            // write script but remove listener before to avoid double update
            storableScript.removeListener(this);
            storableScript.writeScript(codeEditor.getCode());
            storableScript.addListener(this);
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
                return storableScript.readScript();
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
        storableScript.removeListener(this);
    }

    @Override
    public boolean isClosable() {
        if (!saved.get()) {
            return GseAlerts.showSaveDialog(((ProjectFile) storableScript).getName(), this);
        }
        return true;
    }

}
