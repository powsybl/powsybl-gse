/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class AutoCompletion {

    private final Popup completionPopup;

    private ListView<MenuItem> listView;

    private final CodeArea codeArea;

    private List<String> suggestionList;

    public AutoCompletion(CodeArea codeArea) {
        this.codeArea = Objects.requireNonNull(codeArea);
        suggestionList = new ArrayList<>();
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
        showPopup(window);
    }

    public void showMethodsSuggestions(Window window) {
        completeMethod();
        showPopup(window);
    }

    public List<String> getSuggestionList() {
        return suggestionList;
    }

    public void setSuggestionList(List<String> suggestions) {
        suggestionList = suggestions;
    }

    private void completeWord(String lastToken) {
        List<MenuItem> menuItems = new ArrayList<>();
        suggestionList.forEach(str -> {
            if (!lastToken.isEmpty() && str.startsWith(lastToken) && !str.equals(lastToken)) {
                MenuItem menuItem = new MenuItem(str);
                menuItems.add(menuItem);
            }
        });
        refreshListView(menuItems, lastToken.length());
    }

    private void completeMethod() {
        List<MenuItem> metdodsItems = suggestionList.stream().map(MenuItem::new).collect(Collectors.toList());
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

        /* * Each row in a ListView should be 24px tall.  Also, we have to add an extra
         * two px to account for the borders of the ListView.
         * ScrollPane is set to be visible when Items size exceed MAX_ITEMS_SIZE
         */
        final int rowHeight = 24;
        final int maxItemsSize = 13;
        listView.setPrefHeight(menuItems.size() <= maxItemsSize ? menuItems.size() * rowHeight + 2 : 260);
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

        codeArea.replaceText(codeArea.getCaretPosition() - tokenLength, codeArea.getCaretPosition(), text);
        if (selectedItemText.contains("(") && !selectedItemText.substring(selectedItemText.lastIndexOf('(')).equals("()")) {
            codeArea.getCaretSelectionBind().moveTo(codeArea.getCaretPosition() - 1);
        }
        completionPopup.hide();
    }

    public void hidePopup() {
        if (completionPopup != null) {
            completionPopup.hide();
        }
    }

    public void showPopup(Window window) {
        if (!listView.getItems().isEmpty()) {
            codeArea.getCaretBounds().ifPresent(caretBounds -> {
                double positionX = caretBounds.getMinX() - 20;
                double positionY = caretBounds.getMinY() + 20;
                completionPopup.show(window, positionX, positionY);
            });
        }
    }

}
