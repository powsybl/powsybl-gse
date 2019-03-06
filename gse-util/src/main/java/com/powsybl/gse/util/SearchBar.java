/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.commons.PowsyblException;
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
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier at rte-france.com>
 */
public final class SearchBar extends HBox {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SearchBar");

    private final KeyCombination nextKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    private final KeyCombination previousKeyCombination = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN);

    private final CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();
    private final Button closeButton = new Button();
    private final Button upButton;
    private final Button downButton;
    private final CheckBox caseSensitiveBox;
    private final Label caseSensitiveLabel;
    private final Label matchLabel = new Label();
    private MessageFormat nbMatchFound = new MessageFormat(RESOURCE_BUNDLE.getString("NbMatchFound"));
    private PseudoClass failed;

    private Searchable searchedArea;

    private final SearchMatcher matcher = new SearchMatcher();

    private class SearchTuple {
        final int start;
        final int end;

        SearchTuple(int start, int end) {
            this.start = start;
            this.end = end;
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

        void find(String searchPattern, String searchedTxt, boolean caseSensitive) {
            reset();
            try {
                Matcher sensitiveMatcher = caseSensitive ? Pattern.compile(searchPattern).matcher(searchedTxt) : Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE).matcher(searchedTxt);
                while (sensitiveMatcher.find()) {
                    positions.add(new SearchTuple(sensitiveMatcher.start(), sensitiveMatcher.end()));
                }
                nbMatchesProperty.set(positions.size());
                nextMatch();
            } catch (PatternSyntaxException psex) {
                throw new PowsyblException(RESOURCE_BUNDLE.getString("SyntaxError"));
            }
        }

        void reset() {
            positions.clear();
            nbMatchesProperty.set(-1);
            currentMatchProperty.set(-1);
        }

    }

    public SearchBar(Searchable textArea) {
        super(0);

        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.2em");
        Text upGlyph = Glyph.createAwesomeFont('\uf106').size("1.4em");
        Text downGlyph = Glyph.createAwesomeFont('\uf107').size("1.4em");

        upButton = new Button(null, upGlyph);
        downButton = new Button(null, downGlyph);
        caseSensitiveBox = new CheckBox();
        caseSensitiveBox.getStyleClass().add("check-box");
        caseSensitiveBox.setSelected(false);
        matchLabel.getStyleClass().add("match-label");
        caseSensitiveLabel = new Label(RESOURCE_BUNDLE.getString("MatchCase"));
        searchedArea = Objects.requireNonNull(textArea);
        setPrefHeight(20);
        setAlignment(Pos.CENTER_LEFT);
        closeButton.getStyleClass().add("close-button");
        searchField.setLeft(searchGlyph);
        searchField.setPrefWidth(300);
        upButton.getStyleClass().add("transparent-button");
        downButton.getStyleClass().add("transparent-button");
        searchField.getStyleClass().add("search-field");
        failed = PseudoClass.getPseudoClass("fail");
        Pane gluePanel = new Pane();
        setHgrow(gluePanel, Priority.ALWAYS);
        getChildren().addAll(searchField, upButton, downButton, caseSensitiveBox, caseSensitiveLabel, matchLabel, gluePanel, closeButton);
        setMargin(searchField, new Insets(0, 0, 0, 5));
        setMargin(caseSensitiveBox, new Insets(0, 0, 0, 5));
        setMargin(matchLabel, new Insets(0, 0, 0, 25));
        setMargin(closeButton, new Insets(0, 5, 0, 0));

        caseSensitiveBox.selectedProperty().addListener((observable, oldValue, newValue) -> findCaseSensitiveMatches(textArea, newValue));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || "".equals(newValue)) {
                refresh(textArea);
            } else {
                matcher.find(newValue, searchedArea.getText(), caseSensitiveBox.selectedProperty().get());
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
                matchLabel.setText(nbMatchFound.format(new Object[] {matcher.currentMatchProperty.getValue() + 1, newValue}));
            }
        });

        upButton.disableProperty().bind(matcher.isFirstMatch());
        upButton.setOnAction(e -> matcher.previousMatch());

        downButton.disableProperty().bind(matcher.isLastMatch());
        downButton.setOnAction(e -> matcher.nextMatch());

        matcher.currentMatchProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() > -1) {
                matchLabel.setText(nbMatchFound.format(new Object[] {newValue.intValue() + 1, matcher.nbMatchesProperty().getValue()}));
                textArea.select(matcher.currentMatchStart(), matcher.currentMatchEnd());
            }
        });
    }

    private void findCaseSensitiveMatches(Searchable textArea, boolean newValue) {
        refresh(textArea);
        matcher.find(searchField.getText(), searchedArea.getText(), newValue);
    }

    private void refresh(Searchable textArea) {
        matcher.reset();
        matchLabel.setText("");
        textArea.deselect();
        searchField.pseudoClassStateChanged(failed, false);
    }

    public void requestFocus() {
        searchField.requestFocus();
    }

    public void setCloseAction(EventHandler<ActionEvent> eventHandler) {
        closeButton.setOnAction(eventHandler);
    }

    public void setSearchPattern(String pattern) {
        searchField.setText(pattern);
        Platform.runLater(() -> searchField.positionCaret(pattern.length()));
    }

}
