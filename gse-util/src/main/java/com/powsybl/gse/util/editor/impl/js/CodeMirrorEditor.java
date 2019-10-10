package com.powsybl.gse.util.editor.impl.js;

import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.AbstractObservableValueHelper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CodeMirrorEditor extends AbstractCodeEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeMirrorEditor.class);
    private final WebView webview = new WebView();

    private final String editingTemplate;

    private static final String LIB_BASE_PATH = "/js/codemirror-5.49.0";
    private static final List<String> CSS_INCLUDES = Arrays.asList(
            "lib/codemirror.css",
            "addon/hint/show-hint.css",
            "addon/dialog/dialog.css",
            "addon/search/matchesonscrollbar.css",
            "theme/mdn-like.css"
    );
    private static final List<String> JS_INCLUDES = Arrays.asList(
            "lib/codemirror.js",
            "addon/hint/show-hint.js",
            "addon/hint/anyword-hint.js",
            "mode/groovy/groovy.js",
            "addon/comment/comment.js",
            "addon/edit/matchbrackets.js",
            "addon/edit/closebrackets.js",
            "addon/selection/active-line.js",
            "addon/dialog/dialog.js",
            "addon/search/searchcursor.js",
            "addon/search/search.js",
            "addon/scroll/annotatescrollbar.js",
            "addon/search/matchesonscrollbar.js",
            "addon/search/jump-to-line.js"
    );

    private String editingCode = "";
    private AbstractObservableValueHelper<String> codeProperty;
    private AbstractObservableValueHelper<Integer> caretProperty;
    private final Bindings jsBindings = new Bindings();

    public CodeMirrorEditor(Scene scene, List<String> autoCompletion) {
        try {
            String baseHtmlTemplate = IOUtils.toString(getClass().getResource("/html/js-codemirror-editor.html"), Charset.defaultCharset());
            baseHtmlTemplate = baseHtmlTemplate.replace("JAVA_INJECTED_SCRIPTS", JS_INCLUDES.stream().map(include ->
                String.format("<script src=\"%s\"></script>", getClass().getResource(String.format("%s/%s", LIB_BASE_PATH, include)).toExternalForm())
            ).collect(Collectors.joining("\n")));
            baseHtmlTemplate = baseHtmlTemplate.replace("JAVA_INJECTED_STYLESHEETS", CSS_INCLUDES.stream().map(include ->
                    String.format("<link rel=\"stylesheet\" href=\"%s\">", getClass().getResource(String.format("%s/%s", LIB_BASE_PATH, include)).toExternalForm())
            ).collect(Collectors.joining("\n")));
            baseHtmlTemplate = baseHtmlTemplate.replace(
                    "JAVA_INJECTED_BASIC_COMPLETION",
                    autoCompletion
                            .stream()
                            .map(compl -> String.format("\"%s\"", compl))
                            .collect(Collectors.joining(","))
            );
            editingTemplate = baseHtmlTemplate;
        } catch (IOException e) {
            throw new RuntimeException("Should not happend", e);
        }

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

        webview.getEngine().loadContent(applyEditingTemplate());

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
                    JSObject cursorPositionInfo = (JSObject) webview.getEngine().executeScript("editor.getDoc().getCursor();");
                    return (Integer) cursorPositionInfo.getMember("line") + (Integer) cursorPositionInfo.getMember("ch");
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
        try {
            JSObject cursorPositionInfo = (JSObject) webview.getEngine().executeScript("editor.getDoc().getCursor();");
            Integer ch = (Integer) cursorPositionInfo.getMember("ch");
            Integer line = (Integer) cursorPositionInfo.getMember("line");
            return String.format("%d:%d", line, ch);
        } catch (Exception e){
            return "0:0";
        }
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

    public class Bindings {
        public void onChange(){
            codeProperty.fireChange();
        }

        public void log(String text){
            LOGGER.info(text);
        }

        public void onCaretPositionChange(){
            caretProperty.fireChange();
        }
    }
}
