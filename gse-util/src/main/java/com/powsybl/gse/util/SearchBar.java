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
    private final Label matchLabel = new Label();
    private MessageFormat nbMatchFound = new MessageFormat(RESOURCE_BUNDLE.getString("NbMatchFound"));
    private PseudoClass failed;

    private Searchable searchedArea;

    private final ReplaceWordBar replaceWordBar = new ReplaceWordBar();

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

        void find(String searchPattern, String searchedTxt) {
            reset();
            try {
                Matcher matcher = Pattern.compile(searchPattern).matcher(searchedTxt);
                while (matcher.find()) {
                    positions.add(new SearchTuple(matcher.start(), matcher.end()));
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

    private class ReplaceWordBar extends HBox {

        private final Button replaceButton;
        private final Button replaceAllButton;
        private final CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();

        ReplaceWordBar() {
            super(6);
            Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
            searchField.setLeft(searchGlyph);
            searchField.setPrefWidth(300);
            searchField.getStyleClass().add("search-field");
            replaceButton = new Button(RESOURCE_BUNDLE.getString("Replace"));
            replaceAllButton = new Button(RESOURCE_BUNDLE.getString("ReplaceAll"));
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

        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
        Text upGlyph = Glyph.createAwesomeFont('\uf106').size("1.4em");
        Text downGlyph = Glyph.createAwesomeFont('\uf107').size("1.4em");

        upButton = new Button(null, upGlyph);
        downButton = new Button(null, downGlyph);
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
        getChildren().addAll(searchField, upButton, downButton, matchLabel, gluePanel, closeButton);
        setMargin(searchField, new Insets(0, 0, 0, 5));
        setMargin(closeButton, new Insets(0, 5, 0, 0));


        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || "".equals(newValue)) {
                matcher.reset();
                matchLabel.setText("");
                textArea.deselect();
                searchField.pseudoClassStateChanged(failed, false);
            } else {
                matcher.find(newValue, searchedArea.getText());
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

        replaceWordBar.replaceAllButton.disableProperty().bind(validateDisableProperty());
        replaceWordBar.replaceButton.disableProperty().bind(validateDisableProperty());
    }

    private BooleanBinding validateDisableProperty() {
        return getMatcherNbMatchesProperty().isEqualTo(0).or(searchField.textProperty().isEmpty());
    }

    public VBox setMode(String searchMode) {
        VBox vBox = new VBox(6);
        vBox.getChildren().add(this);
        switch (searchMode) {
            case "search":
                return vBox;
            case "replace":
                vBox.getChildren().add(replaceWordBar);
                return vBox;
            default:
                return null;
        }
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

    public int getCurrentMatchEnd() {
        return matcher.currentMatchEnd();
    }

    public IntegerProperty getMatcherNbMatchesProperty() {
        return matcher.nbMatchesProperty;
    }

    public IntegerProperty getMatcherCurrentMatchProperty() {
        return matcher.currentMatchProperty;
    }

    public BooleanBinding isLastMatch() {
        return matcher.isLastMatch();
    }

    public void matcherFind(String searchPattern, String searchedTxt) {
        matcher.find(searchPattern, searchedTxt);
    }

    public String getReplaceText() {
        return replaceWordBar.getSearchField().getText();
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

}
