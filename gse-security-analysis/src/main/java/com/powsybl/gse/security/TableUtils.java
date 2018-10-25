/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public final class TableUtils {

    private TableUtils() {
        // No public empty constructor for utility class
    }

    /**
     * Install the keyboard handler:
     *   + CTRL + C = copy to clipboard
     * @param table
     * @param copyHeaders true if include column header in copied data
     */
    public static void installCopyPasteHandler(TableView<?> table, boolean copyHeaders) {

        // install copy/paste keyboard handler
        table.setOnKeyPressed(new TableKeyEventHandler(copyHeaders));

    }

    /**
     * Copy/Paste keyboard event handler.
     * The handler uses the keyEvent's source for the clipboard data. The source must be of type TableView.
     */
    public static class TableKeyEventHandler implements EventHandler<KeyEvent> {
        private boolean copyHeaders;

        public TableKeyEventHandler(boolean copyHeaders) {
            this.copyHeaders = copyHeaders;
        }

        KeyCodeCombination copyKeyCodeCompination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        public void handle(final KeyEvent keyEvent) {
            if (copyKeyCodeCompination.match(keyEvent) && keyEvent.getSource() instanceof TableView) {
                // copy to clipboard
                copySelectionToClipboard((TableView<?>) keyEvent.getSource(), copyHeaders);

                // event is handled, consume it
                keyEvent.consume();
            }
        }
    }

    /**
     * Get table selection and copy it to the clipboard.
     * @param table
     * @param copyHeaders true if include column header in copied data
     */
    public static void copySelectionToClipboard(TableView<?> table, boolean copyHeaders) {

        StringBuilder clipboardString = new StringBuilder();

        ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();

        int prevRow = -1;

        // Fill headers if header row exists
        if (copyHeaders) {
            AtomicBoolean first = new AtomicBoolean(true);
            positionList.stream().map(TablePosition::getColumn).distinct().sorted().forEach(col -> {
                if (!first.get()) {
                    clipboardString.append('\t');
                }
                clipboardString.append(table.getColumns().get(col).getText());
                first.set(false);
            });
            clipboardString.append('\n');
        }

        // Fill data
        for (TablePosition position : positionList) {

            int row = position.getRow();
            int col = position.getColumn();

            Object cell = (Object) table.getColumns().get(col).getCellData(row);

            // null-check: provide empty string for nulls
            if (cell == null) {
                cell = "";
            }

            // determine whether we advance in a row (tab) or a column
            // (newline).
            if (prevRow == row) {

                clipboardString.append('\t');

            } else if (prevRow != -1) {

                clipboardString.append('\n');

            }

            // create string from cell
            String text = cell.toString();

            // add new item to clipboard
            clipboardString.append(text);

            // remember previous
            prevRow = row;
        }

        // create clipboard content
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(clipboardString.toString());

        // set clipboard content
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }
}
