package com.powsybl.gse.util.editor.impl.js;

import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.impl.swing.AbstractObservableValueHelper;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AceCodeEditor extends AbstractCodeEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AceCodeEditor.class);
    private final WebView webview = new WebView();

    private final String editingTemplate;

    private static final String LIB_BASE_PATH = "/js/ace";
    private static final List<String> CSS_INCLUDES = Collections.emptyList();
    private static final List<String> JS_INCLUDES = Arrays.asList(
            "ace.js",
            "ext-language_tools.js",
            "theme-textmate.js",
            "ext-searchbox.js",
            "ext-textarea.js"
    );

    private String editingCode = "";
    private AbstractObservableValueHelper<String> codeProperty;
    private AbstractObservableValueHelper<Integer> caretProperty;

    public AceCodeEditor(Scene scene, List<String> autoCompletion) {

        try {
            String baseHtmlTemplate = IOUtils.toString(getClass().getResource("/html/js-ace-editor.html"), Charset.defaultCharset());
            baseHtmlTemplate = baseHtmlTemplate.replace("JAVA_INJECTED_SCRIPTS", JS_INCLUDES.stream().map(include ->
                    String.format("<script src=\"%s\"></script>", getClass().getResource(String.format("%s/%s", LIB_BASE_PATH, include)).toExternalForm())
            ).collect(Collectors.joining("\n")));
            baseHtmlTemplate = baseHtmlTemplate.replace("JAVA_INJECTED_STYLESHEETS", CSS_INCLUDES.stream().map(include ->
                    String.format("<link rel=\"stylesheet\" href=\"%s\">", getClass().getResource(String.format("%s/%s", LIB_BASE_PATH, include)).toExternalForm())
            ).collect(Collectors.joining("\n")));
            editingTemplate = baseHtmlTemplate;

        } catch (IOException e) {
            throw new RuntimeException("Should not happend", e);
        }

        webview.getEngine().loadContent(applyEditingTemplate());

        webview.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            LOGGER.info("{}", event);
        });

        webview.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            LOGGER.info("{}", event);
        });

        setMasterNode(webview);
        setShowDetailNode(false);

        initObservableValues();
    }

    private String applyEditingTemplate() {
        return editingTemplate.replace("JAVA_INJECTED_CODE", editingCode);
    }

    /**
     * returns the current code in the editor and updates an editing snapshot of the code which can be reverted to.
     */
    public String getCodeAndSnapshot() {
        this.editingCode = (String) webview.getEngine().executeScript("editor.getValue();");
        return editingCode;
    }

    /**
     * revert edits of the code to the last edit snapshot taken.
     */
    public void revertEdits() {
        setCode(editingCode);
    }

    private void initObservableValues() {
        codeProperty = new AbstractObservableValueHelper<String>() {
            @Override
            public String getValue() {
                return getCode();
            }
        };

//        codeArea.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                if (ignoreDocumentLoadChange) {
//                    ignoreDocumentLoadChange = false;
//                    return;
//                }
//                codeProperty.fireChange();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                codeProperty.fireChange();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                if (ignoreDocumentLoadChange) {
//                    return;
//                }
//                codeProperty.fireChange();
//            }
//        });

        caretProperty = new AbstractObservableValueHelper<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    JSObject cursorPositionInfo = (JSObject) webview.getEngine().executeScript("editor.getDoc().getCursor();");
                    return Integer.parseInt((String) cursorPositionInfo.getMember("line")) + Integer.parseInt((String) cursorPositionInfo.getMember("ch"));
                } catch (Exception e) {
                    return 0;
                }
            }
        };

//        codeArea.getCaret().addChangeListener(e -> {
//            caretProperty.fireChange();
//        });
    }

    @Override
    public void setTabSize(int size) {
        try {
            webview.getEngine().executeScript("editor.setOption('tabSize'," + size + ");");
        } catch (Exception e) {
            // noop
        }

    }

    @Override
    public String getCode() {
        try {
            return (String) webview.getEngine().executeScript("editor.getValue();");
        } catch (Exception e) {
            return "";
        }

    }

    @Override
    public void setCode(String code) {
        this.editingCode = code;
        webview.getEngine().loadContent(applyEditingTemplate());
    }

    @Override
    public ObservableValue<String> codeProperty() {
        return codeProperty;
    }

    @Override
    public ObservableValue<Integer> caretPositionProperty() {
        return caretProperty;
    }

    @Override
    public String currentPosition() {
        return "0";
    }

    @Override
    public void moveCaret(int newPosition) {

    }

    @Override
    public void replace(String text, int rangeStart, int rangeEnd) {

    }

    @Override
    public Pair<Double, Double> caretDisplayPosition() {
        try {
            JSObject cursorPositionInfo = (JSObject) webview.getEngine().executeScript("editor.getDoc().getCursor();");
            return new Pair<>(Double.parseDouble((String) cursorPositionInfo.getMember("line")), Double.parseDouble((String) cursorPositionInfo.getMember("ch")));

        } catch (Exception e) {
            return new Pair<>(0.0, 0.0);
        }
    }

}
