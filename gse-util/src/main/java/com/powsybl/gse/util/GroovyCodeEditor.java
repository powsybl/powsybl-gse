/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.google.common.base.Stopwatch;
import com.powsybl.commons.exceptions.UncheckedClassNotFoundException;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.gse.spi.KeywordsProvider;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.EnergySource;
import groovyjarjarantlr.Token;
import groovyjarjarantlr.TokenStream;
import groovyjarjarantlr.TokenStreamException;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.antlr.GroovySourceToken;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.UnicodeLexerSharedInputState;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.controlsfx.control.MasterDetailPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GroovyCodeEditor extends MasterDetailPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyCodeEditor.class);

    public static final int DEFAULT_TAB_SIZE = 4;

    private final SearchableCodeArea codeArea = new SearchableCodeArea();

    private final KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

    private final KeyCombination replaceWordKeyCombination = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);

    private final SearchBar searchBar;

    private final KeyCombination pasteKeyCombination = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);

    private static final ServiceLoaderCache<KeywordsProvider> KEYWORDS_LOADER = new ServiceLoaderCache<>(KeywordsProvider.class);

    private boolean allowedDrag = false;

    private int tabSize = DEFAULT_TAB_SIZE;

    private AutoCompletion autoCompletion;

    private final List<String> stantardSuggestions = Arrays.asList("as", "assert", "boolean", "break", "breaker", "byte",
            "case", "catch", "char", "class", "continue", "def", "default", "double", "else", "enum",
            "extends", "false", "finally", "float", "for", "generator", "if", "implements", "import", "in",
            "instanceof", "int", "interface", "load", "long", "native", "network", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "substation", "super", "switch", "synchronized", "this",
            "threadsafe", "throw", "throws", "transient", "true", "try", "void", "volatile", "voltageLevel", "while"
    );

    private List<String> optionalSuggestions;

    private static final class SearchableCodeArea extends CodeArea implements Searchable {

        @Override
        public String getText() {
            return super.getText();
        }

        @Override
        public void select(int start, int end) {
            selectRange(start, end);
            showParagraphAtTop(getCurrentParagraph());
        }

        @Override
        public void deselect() {
            super.deselect();
        }
    }

    public GroovyCodeEditor(Scene scene) {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
        searchBar = new SearchBar(codeArea);
        searchBar.setCloseAction(e -> {
            setShowDetailNode(false);
            codeArea.requestFocus();
        });
        setMasterNode(new VirtualizedScrollPane(codeArea));
        VBox vBox = new VBox();
        vBox.getChildren().add(searchBar);
        setDetailNode(vBox);
        setDetailSide(Side.TOP);
        setShowDetailNode(false);

        setOnKeyPressed((KeyEvent ke) -> {
            if (searchKeyCombination.match(ke)) {
                setSearchBar(vBox, "search");
                showDetailNode();
                searchBar.requestFocus();
            } else if (replaceWordKeyCombination.match(ke)) {
                setShowDetailNode(false);
                setSearchBar(vBox, "replace");
                searchBar.setReplaceAllAction(event -> replaceAllOccurences(searchBar.getSearchedText(), codeArea.getText(), searchBar.isCaseSensitiveBoxSelected(), searchBar.isWordSensitiveBoxSelected()));
                searchBar.setReplaceAction(event -> replaceCurrentOccurence(searchBar.getCurrentMatchStart(), searchBar.getCurrentMatchEnd()));
                showDetailNode();
                searchBar.requestFocus();
            }

        });
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::setTabulationSpace);
        codeArea.setOnDragEntered(event -> codeArea.setShowCaret(Caret.CaretVisibility.ON));
        codeArea.setOnDragExited(event -> codeArea.setShowCaret(Caret.CaretVisibility.AUTO));
        codeArea.setOnDragDetected(this::onDragDetected);
        codeArea.setOnDragOver(this::onDragOver);
        codeArea.setOnDragDropped(this::onDragDropped);
        codeArea.setOnSelectionDrag(p -> allowedDrag = true);
    }

    public GroovyCodeEditor(Scene scene, List<String> keywordsSuggestions) {
        this(scene);
        optionalSuggestions = keywordsSuggestions;
        autoCompletion = new AutoCompletion(codeArea);
        codeArea.textProperty().addListener((observable, oldCode, newCode) -> {
            autoCompletion.hide();
            int caretPosition = codeArea.getCaretPosition();
            Matcher nonWordMatcher = caretPosition >= 1 ? Pattern.compile("\\W").matcher(codeArea.getText(caretPosition - 1, caretPosition)) : null;
            Matcher wordMatcher = caretPosition >= 2 ? Pattern.compile("\\w").matcher(codeArea.getText(caretPosition - 2, caretPosition - 1)) : null;
            Matcher whiteSpaceMatcher = caretPosition >= 1 ? Pattern.compile("\\s").matcher(codeArea.getText(caretPosition - 1, caretPosition)) : null;
            String lastToken = nonWordMatcher != null && nonWordMatcher.find() ? nonWordMatcher.group() : getLastToken(caretLineText());
            autoComplete(caretPosition, wordMatcher, whiteSpaceMatcher, lastToken);
        });
    }

    private void autoComplete(int caretPosition, Matcher wordMatcher, Matcher whiteSpaceMatcher, String lastToken) {
        int length = lastToken.length();
        if (lastToken.equals(".") && wordMatcher != null && wordMatcher.find()) {
            List<String> methods = completionMethods().get(getLastToken(caretLineText()));
            showSuggestions("", methods);
        } else if (whiteSpaceMatcher != null && !whiteSpaceMatcher.find() && caretPosition > length + 1 && codeArea.getText(caretPosition - length - 1, caretPosition - length).equals(".")) {
            String[] tokens = caretLineText().split("\\.");
            String text = tokens.length >= 2 ? tokens[tokens.length - 2] : " ";
            String completingMethod = tokens[tokens.length - 1];
            String wordToComplete = getLastToken(text);
            List<String> methods = completionMethods().get(wordToComplete);
            showSuggestions(completingMethod, methods);
        } else {
            List<String> keywordsSuggestions = new ArrayList<>(stantardSuggestions);
            Set<String> energySourceEnums = Arrays.stream(EnergySource.class.getDeclaredFields()).filter(Field::isEnumConstant).map(Field::getName)
                    .collect(Collectors.toSet());
            Set<String> countryEnums = Arrays.stream(Country.class.getDeclaredFields()).filter(Field::isEnumConstant).map(Field::getName)
                    .collect(Collectors.toSet());
            keywordsSuggestions.addAll(energySourceEnums);
            keywordsSuggestions.addAll(countryEnums);
            if (optionalSuggestions != null) {
                keywordsSuggestions.addAll(optionalSuggestions);
            }
            showSuggestions(lastToken, keywordsSuggestions);
        }
    }

    private void showSuggestions(String completiongWord, List<String> suggestions) {
        if (suggestions != null && completiongWord != null) {
            autoCompletion.setSuggestions(suggestions);
            if (completiongWord.equals("")) {
                autoCompletion.showMethodsSuggestions(getScene().getWindow());
            } else {
                if (getScene() != null) {
                    autoCompletion.showKeyWordsSuggestions(completiongWord, getScene().getWindow());
                }
            }
        }
    }

    private static String getLastToken(String text) {
        //extract words from text
        Matcher matcher = Pattern.compile("\\w*\\w").matcher(text);
        String word = " ";
        while (matcher.find()) {
            word = matcher.group();
        }
        return word;
    }

    private String caretLineText() {
        int currentLine = codeArea.getCaretSelectionBind().getParagraphIndex();
        String[] tokenArray = codeArea.getText(currentLine, 0, currentLine, codeArea.getCaretColumn()).split(" ");
        return tokenArray.length >= 1 ? tokenArray[tokenArray.length - 1] : " ";
    }

    private Map<String, List<String>> completionMethods() {
        Map<String, List<String>> completionMethods = new HashMap<>();

        Pair<String, String> networkMap = new Pair<>("com.powsybl.iidm.network.Network", "network");
        Pair<String, String> substationMap = new Pair<>("com.powsybl.iidm.network.Substation", "substation");
        Pair<String, String> voltageLevelMap = new Pair<>("com.powsybl.iidm.network.VoltageLevel", "voltageLevel");
        Pair<String, String> loadMap = new Pair<>("com.powsybl.iidm.network.Load", "load");
        Pair<String, String> generatorMap = new Pair<>("com.powsybl.iidm.network.Generator", "generator");
        Pair<String, String> switchMap = new Pair<>("com.powsybl.iidm.network.Switch", "breaker");

        List<Pair<String, String>> keywordsMap = Arrays.asList(networkMap, substationMap, voltageLevelMap, loadMap, generatorMap, switchMap);
        for (Pair<String, String> pair : keywordsMap) {
            try {
                Class cls = Class.forName(pair.getKey());
                List<Method> methods = Arrays.asList(cls.getMethods());
                List<String> methodNames = methodsWithParameters(methods);
                completionMethods.put(pair.getValue(), methodNames);
            } catch (ClassNotFoundException ex) {
                throw new UncheckedClassNotFoundException(ex);
            }
        }
        return completionMethods;
    }

    private static List<String> methodsWithParameters(List<Method> methods) {
        return methods.stream()
                .map(method -> {
                    String parameters = Arrays.stream(method.getParameters())
                            .map(param -> {
                                Class<?> type = param.getType();
                                String typeName = type.getSimpleName();
                                return !type.isPrimitive() ? typeName + " " + typeName.toLowerCase().replaceAll("\\W", "") : typeName + " " + typeName.substring(0, 1);
                            })
                            .collect(Collectors.joining(", "));
                    return method.getName() + "(" + parameters + ")";
                }).collect(Collectors.toList());
    }

    private void showDetailNode() {
        if (!isShowDetailNode()) {
            setShowDetailNode(true);
        }
    }

    private void setSearchBar(VBox vBox, String searchMode) {
        vBox.getChildren().setAll(searchBar.setMode(searchMode));
        setDetailNode(vBox);
        resetDividerPosition();
        if (codeArea.getSelectedText() != null && !"".equals(codeArea.getSelectedText())) {
            searchBar.setSearchPattern(codeArea.getSelectedText());
        }
    }

    private void replaceAllOccurences(String wordToReplace, String text, boolean caseSensitive, boolean wordSensitive) {
        String replaceText = searchBar.getReplaceText();
        int ci = Pattern.CASE_INSENSITIVE;
        String code;
        if (!wordSensitive) {
            code = caseSensitive ? StringUtils.replacePattern(text, wordToReplace, replaceText) : Pattern.compile(wordToReplace, ci).matcher(text).replaceAll(replaceText);
        } else {
            Matcher matcher = caseSensitive ? Pattern.compile("\\W" + wordToReplace + "\\W").matcher(text) : Pattern.compile("\\W" + wordToReplace + "\\W", ci).matcher(text);
            String txt = text;
            while (matcher.find()) {
                int length = txt.length();
                txt = txt.substring(0, matcher.start() + 1) + replaceText + txt.substring(matcher.end() - 1, length);
                matcher = caseSensitive ? Pattern.compile("\\W" + wordToReplace + "\\W").matcher(txt) : Pattern.compile("\\W" + wordToReplace + "\\W", ci).matcher(txt);
            }
            code = txt;
        }
        codeArea.clear();
        codeArea.replaceText(0, 0, code);
        searchBar.findMatch(searchBar.getSearchedText(), codeArea.getText(), searchBar.isCaseSensitiveBoxSelected(), searchBar.isWordSensitiveBoxSelected());
    }

    private void replaceCurrentOccurence(int startPosition, int endPosition) {
        int lastMatch = searchBar.getCurrentMatchProperty().get();
        codeArea.replaceText(startPosition, endPosition, searchBar.getReplaceText());
        searchBar.findMatch(searchBar.getSearchedText(), codeArea.getText(), searchBar.isCaseSensitiveBoxSelected(), searchBar.isWordSensitiveBoxSelected());
        if (searchBar.getReplaceText().contains(searchBar.getSearchedText())) {
            while (searchBar.getCurrentMatchProperty().get() <= lastMatch) {
                if (searchBar.isLastMatch().get()) {
                    break;
                } else {
                    searchBar.nextMatch();
                }
            }
        }
    }

    private void setTabulationSpace(KeyEvent ke) {
        if (ke.getCode() == KeyCode.TAB) {
            ke.consume();
            int currentLine = codeArea.getCaretSelectionBind().getParagraphIndex();
            int fromLineStartToCaret = codeArea.getText(currentLine, 0, currentLine, codeArea.getCaretColumn()).length();
            codeArea.insertText(codeArea.getCaretPosition(), generateTabSpace(tabSpacesToAdd(fromLineStartToCaret)));
        } else if (pasteKeyCombination.match(ke)) {
            ke.consume();
            deleteSelection();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.getString() != null) {
                try (Scanner sc = new Scanner(clipboard.getString())) {
                    StringBuilder formatClipboard = new StringBuilder();
                    while (sc.hasNextLine()) {
                        replaceTabulation(sc, formatClipboard);
                    }
                    codeArea.insertText(codeArea.getCaretPosition(), formatClipboard.toString());
                }
            }
        }
    }

    private void deleteSelection() {
        if (!codeArea.getSelectedText().isEmpty()) {
            codeArea.deleteText(codeArea.selectionProperty().getValue());
        }
    }

    private void replaceTabulation(Scanner sc, StringBuilder sb) {
        String line = sc.nextLine();
        while (line.contains("\t")) {
            line = line.replaceFirst(Character.toString('\t'), generateTabSpace(tabSpacesToAdd(line.indexOf('\t'))));
        }
        sb.append(line);
        if (sc.hasNextLine()) {
            sb.append("\n");
        }
    }

    public void setTabSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Tabulation size might be strictly positive");
        }
        tabSize = size;
    }

    private int getTabSize() {
        return tabSize;
    }

    private static String generateTabSpace(int size) {
        return StringUtils.repeat(" ", size);
    }

    private int tabSpacesToAdd(int currentPosition) {
        return getTabSize() - (currentPosition % getTabSize());
    }

    private void onDragDetected(MouseEvent event) {
        if (allowedDrag) {
            Dragboard db = codeArea.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(codeArea.getSelectedText());
            db.setContent(content);
            event.consume();
            allowedDrag = false;
        }
    }

    private void onDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if ((db.hasContent(EquipmentInfo.DATA_FORMAT) && db.getContent(EquipmentInfo.DATA_FORMAT) instanceof EquipmentInfo) || db.hasString()) {
            if (event.getGestureSource() == codeArea) {
                event.acceptTransferModes(TransferMode.MOVE);
            } else {
                event.acceptTransferModes(TransferMode.COPY);
            }
            CharacterHit hit = codeArea.hit(event.getX(), event.getY());
            codeArea.displaceCaret(hit.getInsertionIndex());
        }
    }

    private void onDragDropped(DragEvent event) {
        codeArea.setShowCaret(Caret.CaretVisibility.AUTO);
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
            List<EquipmentInfo> equipmentInfoList = (List<EquipmentInfo>) db.getContent(EquipmentInfo.DATA_FORMAT);
            codeArea.insertText(codeArea.getCaretPosition(), equipmentInfoList.get(0).getIdAndName().getId());
            success = true;
        } else if (db.hasString() && event.getGestureSource() != codeArea) {
            codeArea.insertText(codeArea.getCaretPosition(), db.getString());
            success = true;
        } else if (event.getGestureSource() == codeArea) {
            CharacterHit hit = codeArea.hit(event.getX(), event.getY());
            codeArea.moveSelectedText(hit.getInsertionIndex());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public void setCode(String code) {
        codeArea.clear();
        codeArea.replaceText(0, 0, code);
        codeArea.showParagraphAtTop(0);
        codeArea.getUndoManager().forgetHistory();
        resetDividerPosition();
    }

    public String getCode() {
        return codeArea.getText();
    }

    public ObservableValue<String> codeProperty() {
        return codeArea.textProperty();
    }

    public ObservableValue<Integer> caretPositionProperty() {
        return codeArea.caretPositionProperty();
    }

    public String currentPosition() {
        int caretColumn = codeArea.getCaretColumn() + 1;
        int paragraphIndex = codeArea.getCaretSelectionBind().getParagraphIndex() + 1;
        return paragraphIndex + ":" + caretColumn;
    }

    private static String styleClass(int tokenType) {
        switch (tokenType) {
            case GroovyTokenTypes.LCURLY:
            case GroovyTokenTypes.RCURLY:
                return "brace";

            case GroovyTokenTypes.LBRACK:
            case GroovyTokenTypes.RBRACK:
                return "bracket";

            case GroovyTokenTypes.LPAREN:
            case GroovyTokenTypes.RPAREN:
                return "paren";

            case GroovyTokenTypes.SEMI:
                return "semicolon";

            case GroovyTokenTypes.STRING_LITERAL:
            case GroovyTokenTypes.REGEXP_LITERAL:
            case GroovyTokenTypes.DOLLAR_REGEXP_LITERAL:
                return "string";

            case GroovyTokenTypes.ML_COMMENT:
            case GroovyTokenTypes.SH_COMMENT:
            case GroovyTokenTypes.SL_COMMENT:
                return "comment";

            case GroovyTokenTypes.ABSTRACT:
            case GroovyTokenTypes.CLASS_DEF:
            case GroovyTokenTypes.EXTENDS_CLAUSE:
            case GroovyTokenTypes.IMPLEMENTS_CLAUSE:
            case GroovyTokenTypes.IMPORT:
            case GroovyTokenTypes.LITERAL_as:
            case GroovyTokenTypes.LITERAL_assert:
            case GroovyTokenTypes.LITERAL_boolean:
            case GroovyTokenTypes.LITERAL_break:
            case GroovyTokenTypes.LITERAL_byte:
            case GroovyTokenTypes.LITERAL_case:
            case GroovyTokenTypes.LITERAL_catch:
            case GroovyTokenTypes.LITERAL_char:
            case GroovyTokenTypes.LITERAL_class:
            case GroovyTokenTypes.LITERAL_continue:
            case GroovyTokenTypes.LITERAL_def:
            case GroovyTokenTypes.LITERAL_default:
            case GroovyTokenTypes.LITERAL_double:
            case GroovyTokenTypes.LITERAL_else:
            case GroovyTokenTypes.LITERAL_enum:
            case GroovyTokenTypes.LITERAL_extends:
            case GroovyTokenTypes.LITERAL_false:
            case GroovyTokenTypes.LITERAL_finally:
            case GroovyTokenTypes.LITERAL_float:
            case GroovyTokenTypes.LITERAL_for:
            case GroovyTokenTypes.LITERAL_if:
            case GroovyTokenTypes.LITERAL_implements:
            case GroovyTokenTypes.LITERAL_import:
            case GroovyTokenTypes.LITERAL_in:
            case GroovyTokenTypes.LITERAL_instanceof:
            case GroovyTokenTypes.LITERAL_int:
            case GroovyTokenTypes.LITERAL_interface:
            case GroovyTokenTypes.LITERAL_long:
            case GroovyTokenTypes.LITERAL_native:
            case GroovyTokenTypes.LITERAL_new:
            case GroovyTokenTypes.LITERAL_null:
            case GroovyTokenTypes.LITERAL_package:
            case GroovyTokenTypes.LITERAL_private:
            case GroovyTokenTypes.LITERAL_protected:
            case GroovyTokenTypes.LITERAL_public:
            case GroovyTokenTypes.LITERAL_return:
            case GroovyTokenTypes.LITERAL_short:
            case GroovyTokenTypes.LITERAL_static:
            case GroovyTokenTypes.LITERAL_super:
            case GroovyTokenTypes.LITERAL_switch:
            case GroovyTokenTypes.LITERAL_synchronized:
            case GroovyTokenTypes.LITERAL_this:
            case GroovyTokenTypes.LITERAL_threadsafe:
            case GroovyTokenTypes.LITERAL_throw:
            case GroovyTokenTypes.LITERAL_throws:
            case GroovyTokenTypes.LITERAL_transient:
            case GroovyTokenTypes.LITERAL_true:
            case GroovyTokenTypes.LITERAL_try:
            case GroovyTokenTypes.LITERAL_void:
            case GroovyTokenTypes.LITERAL_volatile:
            case GroovyTokenTypes.LITERAL_while:
            case GroovyTokenTypes.PACKAGE_DEF:
            case GroovyTokenTypes.UNUSED_CONST:
            case GroovyTokenTypes.UNUSED_DO:
            case GroovyTokenTypes.UNUSED_GOTO:
            case GroovyTokenTypes.TYPE:
                return "keyword";

            default:
                return null;
        }
    }

    private int length(GroovySourceToken token) {
        int offset1 = codeArea.getDocument().position(token.getLine() -  1, token.getColumn() - 1).toOffset();
        int offset2 = codeArea.getDocument().position(token.getLineLast() - 1, token.getColumnLast() - 1).toOffset();
        return offset2 - offset1;
    }

    private void buildStyle(String styleClass, StyleSpansBuilder<Collection<String>> spansBuilder, int length, Token token) {
        if (styleClass != null) {
            spansBuilder.add(Collections.singleton(styleClass), length);
        } else if (!KEYWORDS_LOADER.getServices().isEmpty()) {
            for (KeywordsProvider styleExtension : KEYWORDS_LOADER.getServices()) {
                String style = styleExtension.styleClass(token.getText());
                if (style != null) {
                    spansBuilder.add(Collections.singleton(style), length);
                } else {
                    spansBuilder.add(Collections.emptyList(), length);
                }
            }
        } else {
            spansBuilder.add(Collections.emptyList(), length);
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        boolean added = false;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        if (!text.isEmpty()) {
            SourceBuffer sourceBuffer = new SourceBuffer();
            try (UnicodeEscapingReader reader = new UnicodeEscapingReader(new StringReader(text), sourceBuffer)) {
                GroovyLexer lexer = new GroovyLexer(new UnicodeLexerSharedInputState(reader));
                lexer.setWhitespaceIncluded(true);
                TokenStream tokenStream = lexer.plumb();
                Token token = tokenStream.nextToken();
                while (token.getType() != Token.EOF_TYPE) {
                    String styleClass = styleClass(token.getType());
                    int length = length((GroovySourceToken) token);
                    buildStyle(styleClass, spansBuilder, length, token);
                    added = true;
                    token = tokenStream.nextToken();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (TokenStreamException e) {
                LOGGER.trace(e.getMessage());
            }
        }

        if (!added) {
            spansBuilder.add(Collections.emptyList(), 0);
        }

        stopwatch.stop();
        LOGGER.trace("Highlighting of {} characters computed in {} ms", text.length(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return spansBuilder.create();
    }
}
