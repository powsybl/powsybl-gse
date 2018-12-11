/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.google.common.base.Stopwatch;
import groovyjarjarantlr.Token;
import groovyjarjarantlr.TokenStream;
import groovyjarjarantlr.TokenStreamException;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.beans.value.ChangeListener;
import org.codehaus.groovy.antlr.GroovySourceToken;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.UnicodeLexerSharedInputState;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.controlsfx.control.MasterDetailPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.*;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GroovyCodeEditor extends MasterDetailPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyCodeEditor.class);

    private final SearchableCodeArea codeArea = new SearchableCodeArea();

    private final KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

    private String caretPositonText;

    private boolean allowedDrag = false;

    private ChangeListener<Integer> positionListener;

    private Set<Integer> positionsList = new HashSet<>();

    private boolean searchingForBracketMatches = false;

    private int matchedPosition;

    private static final String MATCHED_BRACKET_STYLE = "bracket-matches";

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
        SearchBar searchBar = new SearchBar(codeArea);
        searchBar.setCloseAction(e -> {
            setShowDetailNode(false);
            codeArea.requestFocus();
        });
        setMasterNode(new VirtualizedScrollPane(codeArea));
        setDetailNode(searchBar);
        setDetailSide(Side.TOP);
        setShowDetailNode(false);

        setOnKeyPressed((KeyEvent ke) -> {
            if (searchKeyCombination.match(ke)) {
                if (codeArea.getSelectedText() != null && !"".equals(codeArea.getSelectedText())) {
                    searchBar.setSearchPattern(codeArea.getSelectedText());
                }
                if (!isShowDetailNode()) {
                    setShowDetailNode(true);
                }
                searchBar.requestFocus();
            }

        });

        codeArea.setOnDragEntered(event -> codeArea.setShowCaret(Caret.CaretVisibility.ON));
        codeArea.setOnDragExited(event -> codeArea.setShowCaret(Caret.CaretVisibility.AUTO));
        codeArea.setOnDragDetected(this::onDragDetected);
        codeArea.setOnDragOver(this::onDragOver);
        codeArea.setOnDragDropped(this::onDragDropped);
        codeArea.setOnSelectionDrag(p -> allowedDrag = true);

        positionListener = (observable, oldvalue, newvalue) -> {
            searchingForBracketMatches = false;
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
            if (newvalue > 0) {
                caretPositonText = codeArea.getText(newvalue - 1, newvalue);
                findBracketsMatches(codeArea.getText());
            }
        };
        codeArea.caretPositionProperty().addListener(positionListener);

        codeProperty().addListener((observable, oldvalue, newvalue) -> {
            searchingForBracketMatches = false;
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
        });


    }

    private void findBracketsMatches(String text) {
        if (caretPositonText != null) {
            if (caretPositonText.equals(Character.toString(')'))) {
                highlightLeftBracketsMatches(text, ')', '(');
            } else if (caretPositonText.equals(Character.toString('}'))) {
                highlightLeftBracketsMatches(text, '}', '{');
            } else if (caretPositonText.equals(Character.toString(']'))) {
                highlightLeftBracketsMatches(text, ']', '[');
            } else if (caretPositonText.equals(Character.toString('('))) {
                highlightRightBracketsMatches(text, '(', ')');
            } else if (caretPositonText.equals(Character.toString('{'))) {
                highlightRightBracketsMatches(text, '{', '}');
            } else if (caretPositonText.equals(Character.toString('['))) {
                highlightRightBracketsMatches(text, '[', ']');
            }
        }

    }

    private void highlightLeftBracketsMatches(String text, char c1, char c2) {
        Integer value = codeArea.caretPositionProperty().getValue();
        int counter = 0;
        if (positionsList.contains(value - 1)) {
            for (int i = value - 2; i >= 0; i--) {
                if (text.charAt(i) == c1 && positionsList.contains(i)) {
                    counter++;
                } else if (text.charAt(i) == c2 && positionsList.contains(i)) {
                    if (counter == 0) {
                        setHighlightBracketStyle(i, value);
                        break;
                    } else if (counter == 1) {
                        counter = 0;
                    } else {
                        counter--;
                    }
                }
            }
        }
    }

    private void highlightRightBracketsMatches(String text, char c1, char c2) {
        Integer value = codeArea.caretPositionProperty().getValue();
        int counter = 0;
        if (positionsList.contains(value - 1)) {
            for (int i = value; i < text.length(); i++) {
                if (text.charAt(i) == c1 && positionsList.contains(i)) {
                    counter++;
                } else if (text.charAt(i) == c2 && positionsList.contains(i)) {
                    if (counter == 0) {
                        setHighlightBracketStyle(i, value);
                        break;
                    } else if (counter == 1) {
                        counter = 0;
                    } else {
                        counter--;
                    }
                }
            }
        }
    }

    private void setHighlightBracketStyle(int num, int value) {
        searchingForBracketMatches = true;
        matchedPosition = num;
        codeArea.setStyle(num, num + 1, Collections.singletonList(MATCHED_BRACKET_STYLE));
        codeArea.setStyle(value - 1, value, Collections.singletonList(MATCHED_BRACKET_STYLE));
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
        resetDividerPosition();
    }

    public String getCode() {
        return codeArea.getText();
    }

    public ObservableValue<String> codeProperty() {
        return codeArea.textProperty();
    }

    private static String bracketMatchesStyleClass(int tokenType) {
        switch (tokenType) {
            case GroovyTokenTypes.LCURLY:
            case GroovyTokenTypes.RCURLY:
            case GroovyTokenTypes.LBRACK:
            case GroovyTokenTypes.RBRACK:
            case GroovyTokenTypes.LPAREN:
            case GroovyTokenTypes.RPAREN:
                return MATCHED_BRACKET_STYLE;
            default:
                return null;
        }
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

    private String getStyleClass(Token token2) {
        String style = "";
        style = !searchingForBracketMatches ? styleClass(token2.getType()) : bracketMatchesStyleClass(token2.getType());
        return style;
    }

    private void addPostion(int postion) {
        if (!searchingForBracketMatches) {
            positionsList.add(postion);
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        String textUsed = text;
        positionsList.clear();
        if (searchingForBracketMatches) {
            textUsed = codeArea.getText(matchedPosition, matchedPosition + 1);
        }
        Stopwatch stopwatch = Stopwatch.createStarted();

        boolean added = false;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        if (!textUsed.isEmpty()) {
            SourceBuffer sourceBuffer = new SourceBuffer();
            try (UnicodeEscapingReader reader = new UnicodeEscapingReader(new StringReader(textUsed), sourceBuffer)) {
                GroovyLexer lexer = new GroovyLexer(new UnicodeLexerSharedInputState(reader));
                lexer.setWhitespaceIncluded(true);
                TokenStream tokenStream = lexer.plumb();
                Token token = tokenStream.nextToken();
                while (token.getType() != Token.EOF_TYPE) {
                    String styleClass = getStyleClass(token);
                    int length = length((GroovySourceToken) token);
                    if (styleClass != null) {
                        spansBuilder.add(Collections.singleton(styleClass), length);
                        int tokenPostion = codeArea.getDocument().position(token.getLine() - 1, token.getColumn() - 1).toOffset();
                        addPostion(tokenPostion);
                    } else {
                        spansBuilder.add(Collections.emptyList(), length);
                    }
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
        LOGGER.trace("Highlighting of {} characters computed in {} ms", textUsed.length(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return spansBuilder.create();
    }

}
