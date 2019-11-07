/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.gse.util.editor;

import com.powsybl.commons.PowsyblException;
import com.powsybl.gse.util.Glyph;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.powsybl.gse.util.editor.SearchBar.SearchMode.REPLACE;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
public final class SearchBar extends VBox {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SearchBar");
    private static final int DEFAULT_PREF_HEIGHT = 32;
    private static final int FULL_PREF_HEIGHT = 62;
    private final KeyCombination nextKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    private final KeyCombination previousKeyCombination = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN);

    private final CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();
    private final Button closeButton = new Button();
    private final Button upButton;
    private final Button downButton;
    private final CheckBox caseSensitiveBox;
    private final CheckBox wordSensitiveBox;
    private final Label matchLabel = new Label();
    private MessageFormat nbMatchFound = new MessageFormat(RESOURCE_BUNDLE.getString("NbMatchFound"));
    private PseudoClass failed;

    private Searchable searchedArea;

    private final ReplaceWordBar replaceWordBar = new ReplaceWordBar();

    private final SearchMatcher matcher = new SearchMatcher();

    public class SearchTuple {
        final int start;
        final int end;

        SearchTuple(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "Tuple{start=" + start + ", end=" + end + '}';
        }
    }

    private class SearchMatcher {
        private IntegerProperty nbMatchesProperty = new SimpleIntegerProperty();
        private IntegerProperty currentMatchProperty = new SimpleIntegerProperty();
        List<SearchTuple> positions = new ArrayList<>();

        SearchMatcher() {
            reset();
        }

        void nextMatch() {
            int nbMatches = nbMatchesProperty.get();
            int currentMatch = currentMatchProperty.get();
            if (nbMatches > 0 && currentMatch < nbMatches - 1) {
                currentMatchProperty.set(currentMatch + 1);
            }
        }

        void previousMatch() {
            int currentMatch = currentMatchProperty.get();
            if (currentMatch > 0) {
                currentMatchProperty.set(currentMatch - 1);
            }
        }

        IntegerProperty nbMatchesProperty() {
            return nbMatchesProperty;
        }

        IntegerProperty currentMatchProperty() {
            return currentMatchProperty;
        }

        BooleanBinding isFirstMatch() {
            return Bindings.lessThanOrEqual(currentMatchProperty, 0);
        }

        BooleanBinding isLastMatch() {
            return Bindings.lessThanOrEqual(Bindings.subtract(nbMatchesProperty, currentMatchProperty), 1);
        }

        int currentMatchStart() {
            return positions.get(currentMatchProperty.get()).start;
        }

        int currentMatchEnd() {
            return positions.get(currentMatchProperty.get()).end;
        }

        void find(String searchPattern, String searchedTxt, boolean caseSensitive, boolean wordSensitive) {
            String value = escapeMetaCharacters(searchPattern);
            reset();
            try {
                Matcher sensitiveMatcher;
                if (!wordSensitive) {
                    sensitiveMatcher = caseSensitive ? Pattern.compile(value).matcher(searchedTxt) : Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(searchedTxt);
                } else {
                    sensitiveMatcher = caseSensitive ? Pattern.compile("\\W" + value + "\\W").matcher(searchedTxt) : Pattern.compile("\\W" + value + "\\W", Pattern.CASE_INSENSITIVE).matcher(searchedTxt);
                }
                while (sensitiveMatcher.find()) {
                    positions.add(wordSensitive ? new SearchTuple(sensitiveMatcher.start() + 1, sensitiveMatcher.end() - 1) : new SearchTuple(sensitiveMatcher.start(), sensitiveMatcher.end()));
                }
                nbMatchesProperty.set(positions.size());
                nextMatch();
            } catch (PatternSyntaxException psex) {
                throw new PowsyblException(RESOURCE_BUNDLE.getString("SyntaxError"));
            }
        }

        void updateAfterCurrentReplacement() {
            int replacementDiff = getReplaceText().length() - getSearchedText().length();
            int totalMatches = nbMatchesProperty.intValue();
            if (totalMatches > 1) {
                int currentMatch = currentMatchProperty.intValue();
                List<SearchTuple> newPositions = positions.stream().limit(currentMatch).collect(Collectors.toList());
                for (int i = currentMatch + 1; i < totalMatches; i++) {
                    newPositions.add(new SearchTuple(positions.get(i).start + replacementDiff, positions.get(i).end + replacementDiff));
                }
                positions = newPositions;
                nbMatchesProperty.set(positions.size());
                if (currentMatch >= nbMatchesProperty.intValue()) {
                    currentMatchProperty.set(0);
                }
                searchedArea.select(getCurrentMatchStart(), getCurrentMatchEnd());
            } else {
                refreshFind();
            }
        }

        void reset() {
            positions.clear();
            nbMatchesProperty.set(-1);
            currentMatchProperty.set(-1);
        }

    }

    private class ReplaceWordBar extends HBox {

        private final Button replaceButton;
        private final Button replaceAllButton;
        private final CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();

        ReplaceWordBar() {
            super(6);
            VBox.setMargin(this, new Insets(5, 0, 0, 0));
            designSearchField(searchField);
            replaceButton = new Button(RESOURCE_BUNDLE.getString("Replace"));
            replaceAllButton = new Button(RESOURCE_BUNDLE.getString("ReplaceAll"));
            replaceButton.getStyleClass().add("replace-button");
            replaceAllButton.getStyleClass().add("replace-button");
            searchField.setOnKeyPressed((KeyEvent ke) -> {
                if (ke.getCode() == KeyCode.ENTER) {
                    replaceButton.fire();
                }
            });
            setMargin(searchField, new Insets(0, 0, 0, 5));
            getChildren().addAll(searchField, replaceButton, replaceAllButton);

        }

        CustomTextField getSearchField() {
            return searchField;
        }
    }

    public SearchBar(Searchable textArea) {
        super(0);

        Text upGlyph = Glyph.createAwesomeFont('\uf106').size("1.4em");
        Text downGlyph = Glyph.createAwesomeFont('\uf107').size("1.4em");

        upButton = new Button(null, upGlyph);
        downButton = new Button(null, downGlyph);
        caseSensitiveBox = new CheckBox(RESOURCE_BUNDLE.getString("MatchCase"));
        wordSensitiveBox = new CheckBox(RESOURCE_BUNDLE.getString("Words"));
        matchLabel.getStyleClass().add("match-label");
        searchedArea = Objects.requireNonNull(textArea);
        setPrefHeight(DEFAULT_PREF_HEIGHT);
        setAlignment(Pos.CENTER_LEFT);
        closeButton.getStyleClass().add("close-button");
        designSearchField(searchField);
        upButton.getStyleClass().add("transparent-button");
        downButton.getStyleClass().add("transparent-button");
        failed = PseudoClass.getPseudoClass("fail");

        HBox findComponents = new HBox();
        findComponents.setAlignment(Pos.CENTER_LEFT);
        Pane gluePanel = new Pane();
        HBox.setHgrow(gluePanel, Priority.ALWAYS);
        findComponents.getChildren().addAll(searchField, upButton, downButton, caseSensitiveBox, wordSensitiveBox, matchLabel, gluePanel, closeButton);
        getChildren().addAll(findComponents);
        HBox.setMargin(searchField, new Insets(0, 0, 0, 5));
        HBox.setMargin(caseSensitiveBox, new Insets(0, 0, 0, 5));
        HBox.setMargin(wordSensitiveBox, new Insets(0, 0, 0, 8));
        HBox.setMargin(matchLabel, new Insets(0, 0, 0, 25));
        HBox.setMargin(closeButton, new Insets(0, 5, 0, 0));
        // To force next tab order to replace field if present
        upButton.setFocusTraversable(false);
        downButton.setFocusTraversable(false);
        caseSensitiveBox.setFocusTraversable(false);
        wordSensitiveBox.setFocusTraversable(false);
        closeButton.setFocusTraversable(false);

        caseSensitiveBox.selectedProperty().addListener((observable, oldValue, newValue) -> findCaseSensitiveMatches(textArea, newValue));
        wordSensitiveBox.selectedProperty().addListener((observable, oldValue, newValue) -> findWordSensitiveMatches(textArea, newValue));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || "".equals(newValue)) {
                refresh(textArea);
            } else {
                matcher.find(newValue, searchedArea.getText(), caseSensitiveBox.selectedProperty().get(), wordSensitiveBox.selectedProperty().get());
            }
        });

        searchField.setOnKeyPressed((KeyEvent ke) -> {
            if (nextKeyCombination.match(ke)) {
                downButton.fire();
                ke.consume();
            } else if (previousKeyCombination.match(ke)) {
                upButton.fire();
                ke.consume();
            }
        });

        matcher.nbMatchesProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                textArea.deselect();
                matchLabel.setText(RESOURCE_BUNDLE.getString("NoMatchFound"));
                searchField.pseudoClassStateChanged(failed, true);
            } else {
                searchField.pseudoClassStateChanged(failed, false);
                matchLabel.setText(nbMatchFound.format(new Object[]{matcher.currentMatchProperty.getValue() + 1, newValue}));
            }
        });

        upButton.disableProperty().bind(matcher.isFirstMatch());
        upButton.setOnAction(e -> matcher.previousMatch());

        downButton.disableProperty().bind(matcher.isLastMatch());
        downButton.setOnAction(e -> matcher.nextMatch());

        matcher.currentMatchProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() > -1) {
                matchLabel.setText(nbMatchFound.format(new Object[]{newValue.intValue() + 1, matcher.nbMatchesProperty().getValue()}));
                textArea.select(matcher.currentMatchStart(), matcher.currentMatchEnd());
            }
        });

        replaceWordBar.replaceAllButton.disableProperty().bind(validateDisableProperty());
        replaceWordBar.replaceButton.disableProperty().bind(validateDisableProperty());

        searchField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.ENTER.equals(event.getCode())) {
                refreshFind();
            }
        });
    }

    public static String escapeMetaCharacters(String value) {
        List<Character> metaCharacters = Arrays.asList('.', '*', '\\', '(', ')', '[', ']', '{', '}', '^', '$', '|', '+', '?');
        String replacement = value;
        for (Character ch : metaCharacters) {
            if (value.contains(Character.toString(ch))) {
                replacement = replacement.replace(Character.toString(ch), "\\" + ch);
            }
        }
        return replacement;
    }

    private BooleanBinding validateDisableProperty() {
        return getNbMatchesProperty().isEqualTo(0).or(searchField.textProperty().isEmpty());
    }

    public void setMode(SearchMode searchMode) {
        setPrefHeight(DEFAULT_PREF_HEIGHT);
        getChildren().remove(replaceWordBar);
        if (REPLACE.equals(searchMode)) {
            setPrefHeight(FULL_PREF_HEIGHT);
            getChildren().add(replaceWordBar);
        }
    }

    private static void designSearchField(CustomTextField searchField) {
        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.2em");
        searchField.setLeft(searchGlyph);
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
    }

    public boolean isCaseSensitiveBoxSelected() {
        return caseSensitiveBox.isSelected();
    }

    public boolean isWordSensitiveBoxSelected() {
        return wordSensitiveBox.isSelected();
    }

    public String getSearchedText() {
        return searchField.getText();
    }

    public void nextMatch() {
        matcher.nextMatch();
    }

    public void previousMatch() {
        matcher.previousMatch();
    }

    public int getCurrentMatchStart() {
        return matcher.currentMatchStart();
    }

    public List<SearchTuple> getMatchPositions() {
        return matcher.positions;
    }

    public int getCurrentMatchEnd() {
        return matcher.currentMatchEnd();
    }

    public IntegerProperty getNbMatchesProperty() {
        return matcher.nbMatchesProperty;
    }

    public IntegerProperty getCurrentMatchProperty() {
        return matcher.currentMatchProperty;
    }

    public BooleanBinding isLastMatch() {
        return matcher.isLastMatch();
    }

    public void findMatch(String searchPattern, String searchText, boolean caseSensitive, boolean wordSensitive) {
        matcher.find(searchPattern, searchText, caseSensitive, wordSensitive);
    }

    public String getReplaceText() {
        return replaceWordBar.getSearchField().getText();
    }

    private void findCaseSensitiveMatches(Searchable textArea, boolean newValue) {
        refresh(textArea);
        matcher.find(searchField.getText(), searchedArea.getText(), newValue, wordSensitiveBox.selectedProperty().get());
    }

    private void findWordSensitiveMatches(Searchable textArea, boolean newValue) {
        refresh(textArea);
        matcher.find(searchField.getText(), searchedArea.getText(), caseSensitiveBox.selectedProperty().get(), newValue);
    }

    private void refresh(Searchable textArea) {
        matcher.reset();
        matchLabel.setText("");
        textArea.deselect();
        searchField.pseudoClassStateChanged(failed, false);
    }

    public void updateAfterCurrentReplacement() {
        matcher.updateAfterCurrentReplacement();
    }

    public void requestFocus() {
        searchField.requestFocus();
    }

    public void setCloseAction(EventHandler<ActionEvent> eventHandler) {
        closeButton.setOnAction(eventHandler);
    }

    public void setReplaceAllAction(EventHandler<ActionEvent> eventHandler) {
        replaceWordBar.replaceAllButton.setOnAction(eventHandler);
    }

    public void setReplaceAction(EventHandler<ActionEvent> eventHandler) {
        replaceWordBar.replaceButton.setOnAction(eventHandler);
    }

    public void setSearchPattern(String pattern) {
        searchField.setText(pattern);
        Platform.runLater(() -> searchField.positionCaret(pattern.length()));
    }

    public void refreshFind() {
        matcher.find(searchField.getText(), searchedArea.getText(), caseSensitiveBox.isSelected(), wordSensitiveBox.selectedProperty().get());
    }

    public static enum SearchMode {
        SEARCH,
        REPLACE
    }
}
