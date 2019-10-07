package com.powsybl.gse.util.editor.impl.swing;

import com.google.common.collect.ImmutableList;
import com.powsybl.gse.util.editor.AbstractCodeEditor;
import com.powsybl.gse.util.editor.impl.SearchBar;
import com.powsybl.gse.util.editor.Searchable;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.util.Pair;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.*;

public class AlternateCodeEditor extends AbstractCodeEditor implements Searchable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlternateCodeEditor.class);
    private static final List<String> STANDARD_SUGGESTIONS = ImmutableList.of("as", "assert", "boolean", "break", "breaker", "byte",
            "case", "catch", "char", "class", "continue", "def", "default", "double", "else", "enum",
            "extends", "false", "finally", "float", "for", "generator", "if", "implements", "import", "in",
            "instanceof", "int", "interface", "load", "long", "native", "network", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "substation", "super", "switch", "synchronized", "this",
            "threadsafe", "throw", "throws", "transient", "true", "try", "void", "volatile", "voltageLevel", "while"
    );
    private final KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    private final KeyCombination replaceWordKeyCombination = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
    private final RSyntaxTextArea codeArea;
    private final SearchBar searchBar;
    private AbstractObservableValueHelper<String> codeProperty;
    private AbstractObservableValueHelper<Integer> caretProperty;

    public AlternateCodeEditor(Scene scene, List<String> autocompleteList) {
        // Code editor
        codeArea = new RSyntaxTextArea(20, 60);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        codeArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(codeArea);
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(sp);

        // Search bar
        searchBar = new SearchBar(this);
        searchBar.setReplaceAllAction(event -> replaceAllOccurences());
        searchBar.setReplaceAction(event -> replaceCurrentOccurence());
        searchBar.setCloseAction(e -> {
            setShowDetailNode(false);
            codeArea.requestFocus();
        });

        // fixes
        fixSwingIntegrationBug(swingNode, scene);

        // Parent layout setup
        setDetailNode(searchBar);
        setMasterNode(swingNode);
        resetDividerPosition();
        setAnimated(true);
        setDetailSide(Side.TOP);
        setShowDetailNode(false);
        setOnKeyPressed((KeyEvent ke) -> {
            if (searchKeyCombination.match(ke)) {
                searchBar.setMode(SearchBar.SearchMode.SEARCH);
                resetDividerPosition();
                setShowDetailNode(true);
                searchBar.requestFocus();
            } else if (replaceWordKeyCombination.match(ke)) {
                searchBar.setMode(SearchBar.SearchMode.REPLACE);
                resetDividerPosition();
                setShowDetailNode(true);
                searchBar.requestFocus();
            }
        });

        // Autocompletion
        List<String> autocompleteListAndDefault =  new ArrayList<>(STANDARD_SUGGESTIONS);
        autocompleteListAndDefault.addAll(Optional.of(autocompleteList).orElse(Collections.emptyList()));
        CompletionProvider provider = createCompletionProvider(autocompleteListAndDefault);
        org.fife.ui.autocomplete.AutoCompletion ac = new org.fife.ui.autocomplete.AutoCompletion(provider);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(200);
        ac.install(codeArea);

        // Observable values
        initObservableValues();
    }

    private void replaceCurrentOccurence() {
        // No match => -1
        if (searchBar.getCurrentMatchProperty().intValue() >= 0) {
            codeArea.replaceRange(searchBar.getReplaceText(), searchBar.getCurrentMatchStart(), searchBar.getCurrentMatchEnd());
            searchBar.updateAfterCurrentReplacement();
        }
    }

    private void replaceAllOccurences() {
        int replacementDiff = searchBar.getReplaceText().length() - searchBar.getSearchedText().length();
        for (int i = 0; i < searchBar.getNbMatchesProperty().intValue(); i++) {
            codeArea.replaceRange(
                    searchBar.getReplaceText(),
                    searchBar.getMatchPositions().get(i).getStart() + (i * replacementDiff),
                    searchBar.getMatchPositions().get(i).getEnd() + (i * replacementDiff)
            );
        }
        searchBar.refreshFind();
    }

    private void fixSwingIntegrationBug(SwingNode swingNode, Scene scene) {
        swingNode.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.DELETE.equals(event.getCode())) {
                event.consume();
            }
        });
        swingNode.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (scene != null) {
                if (this.equals(scene.getFocusOwner())) {
                    return;
                }
            }
            swingNode.requestFocus();
        });
    }

    private CompletionProvider createCompletionProvider(List<String> completions) {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        provider.setAutoActivationRules(true, ".$");
        completions
                .stream()
                .map(compl -> new BasicCompletion(provider, compl))
                .forEach(provider::addCompletion);
        return provider;
    }

    @Override
    public void setTabSize(int size) {
        codeArea.setTabSize(size);
    }

    @Override
    public void setCode(String code) {
        codeArea.setText(code);
    }

    @Override
    public String getCode() {
        return codeArea.getText();
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
        return codeArea.getCaretLineNumber() + ":" + codeArea.getCaretOffsetFromLineStart();
    }

    @Override
    public void moveCaret(int newPosition) {
        codeArea.moveCaretPosition(newPosition);
    }

    @Override
    public void replace(String text, int rangeStart, int rangeEnd) {
        codeArea.replaceRange(text, rangeStart, rangeEnd);
    }

    @Override
    public Pair<Double, Double> caretDisplayPosition() {
        return new Pair<>(
                codeArea.getCaret().getMagicCaretPosition().getX(),
                codeArea.getCaret().getMagicCaretPosition().getY()
        );
    }

    /**
     * Initialize binding between swing component event listener and javafx observable values
     */
    private void initObservableValues() {
        codeProperty = new AbstractObservableValueHelper<String>() {
            @Override
            public String getValue() {
                return codeArea.getText();
            }
        };

        codeArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                codeProperty.fireChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                codeProperty.fireChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                codeProperty.fireChange();
            }
        });

        caretProperty = new AbstractObservableValueHelper<Integer>() {
            @Override
            public Integer getValue() {
                return codeArea.getCaretPosition();
            }
        };

        codeArea.getCaret().addChangeListener(e -> {
            caretProperty.fireChange();
        });
    }

    @Override
    public String getText() {
        return codeArea.getText();
    }

    @Override
    public void deselect() {
        codeArea.select(codeArea.getSelectionStart(), codeArea.getSelectionStart());
    }

    @Override
    public void select(int start, int end) {
        codeArea.select(start, end);
    }
}

