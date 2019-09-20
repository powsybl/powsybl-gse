/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util.editor.impl.javafx;

import com.powsybl.gse.util.editor.AbstractCodeEditor;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Pair;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fxmisc.richtext.CodeArea;

import java.security.acl.LastOwnerException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class AutoCompletion {

    private final Popup completionPopup;

    private final ListView<MenuItem> listView;

    private final AbstractCodeEditor codeArea;

    private Set<String> suggestions;

    private static final int ROW_HEIGHT = 24;

    private static  final int MAX_ITEMS_SIZE = 13;

    private static final int MAX_HEIGHT = 260;

    public AutoCompletion(AbstractCodeEditor codeEditor) {
        this.codeArea = Objects.requireNonNull(codeEditor);
        suggestions = new HashSet<>();
        listView = new ListView<>();
        listView.setMaxHeight(260);
        listView.setMinWidth(400);
        listView.setCellFactory(lv -> listViewCellFactory());

        completionPopup = new Popup();
        completionPopup.getContent().add(new ScrollPane(new VBox(listView)));
        completionPopup.setAutoHide(true);
    }

    public void showKeyWordsSuggestions(String wordToComplete, Window window) {
        completeWord(wordToComplete);
        show(window);
    }

    public void showMethodsSuggestions(Window window) {
        completeMethod();
        show(window);
    }

    public Set<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new HashSet<>(suggestions);
    }

    private void completeWord(String currentToken) {
        List<MenuItem> menuItems = new ArrayList<>();
        suggestions.forEach(str -> {
            if (!currentToken.isEmpty() && str.startsWith(currentToken) && !str.equals(currentToken)) {
                MenuItem menuItem = new MenuItem(str);
                menuItems.add(menuItem);
            }
        });
        refreshListView(menuItems, currentToken.length());
    }

    private void completeMethod() {
        List<MenuItem> metdodsItems = suggestions.stream().map(MenuItem::new).collect(Collectors.toList());
        refreshListView(metdodsItems, 0);
    }

    private void refreshListView(List<MenuItem> menuItems, int tokenLength) {
        listView.setItems(FXCollections.observableArrayList(menuItems));
        listView.getSelectionModel().selectFirst();
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                menuItemOnAction(tokenLength);
            }
        });
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                menuItemOnAction(tokenLength);
            }
        });
        listView.setPrefHeight(menuItems.size() <= MAX_ITEMS_SIZE ? menuItems.size() * ROW_HEIGHT + 2 : MAX_HEIGHT);
    }

    private static ListCell<MenuItem> listViewCellFactory() {
        return new ListCell<MenuItem>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.getText());
                }
            }
        };
    }

    private void menuItemOnAction(int tokenLength) {
        String selectedItemText = listView.getSelectionModel().getSelectedItem().getText();
        String text = selectedItemText.contains("(") ? selectedItemText.substring(0, selectedItemText.indexOf('(')) + "()" : selectedItemText;
        Integer caretPosition = codeArea.caretPositionProperty().getValue();
        codeArea.replace(text, caretPosition - tokenLength, caretPosition);
        if (selectedItemText.contains("(") && !selectedItemText.substring(selectedItemText.lastIndexOf('(')).equals("()")) {
            codeArea.moveCaret(caretPosition - 1);
        }
        completionPopup.hide();
    }

    public void hide() {
        if (completionPopup != null) {
            completionPopup.hide();
        }
    }

    public void show(Window window) {
        if (!listView.getItems().isEmpty()) {
            Pair<Double, Double> caretDisplayPosition = codeArea.caretDisplayPosition();
            if (caretDisplayPosition != null) {
                completionPopup.show(window, caretDisplayPosition.getKey() - 20, caretDisplayPosition.getValue() + 20);
            }
        }
    }

}
