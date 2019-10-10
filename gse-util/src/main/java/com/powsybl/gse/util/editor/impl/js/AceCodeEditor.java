package com.powsybl.gse.util.editor.impl.js;

import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.AbstractObservableValueHelper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Bindings jsBindings = new Bindings();

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
            if (KeyCode.ENTER.equals(event.getCode())) {
                webview.getEngine().executeScript("editor.insert('\\n');");
            }
        });

        webview.getEngine().getLoadWorker().stateProperty().addListener(
                (ChangeListener) (observable, oldValue, newValue) -> {
                    if (newValue != Worker.State.SUCCEEDED) { return; }

                    JSObject window = (JSObject) webview.getEngine().executeScript("window");
                    // Maintain a strong reference to prevent garbage collection:
                    // https://bugs.openjdk.java.net/browse/JDK-8154127
                    window.setMember("javaBindings", jsBindings);
//                    webview.getEngine().executeScript("console.log = function(message)\n" +
//                            "{\n" +
//                            "    javaBindings.log(message);\n" +
//                            "};");
                }
        );

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

    private void initObservableValues() {
        codeProperty = new AbstractObservableValueHelper<String>() {
            @Override
            public String getValue() {
                return getCode();
            }
        };

        caretProperty = new AbstractObservableValueHelper<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    JSObject cursorPositionInfo = (JSObject) webview.getEngine().executeScript("editor.getCursorPosition();");
                    return (Integer) cursorPositionInfo.getMember("row") + (Integer) cursorPositionInfo.getMember("column");
                } catch (Exception e) {
                    return 0;
                }
            }
        };
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
        return null;
    }

    public class Bindings{
        public void onChange(){
            codeProperty.fireChange();
        }

        public void onCaretPositionChange(){
            caretProperty.fireChange();
        }
    }

}
