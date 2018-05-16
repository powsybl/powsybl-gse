/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TaskMonitorPane extends BorderPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.TaskMonitorPane");

    private static class TaskListCell extends ListCell<TaskItem> {

        private final Label label = new Label("");

        private final ProgressBar progressBar = new ProgressBar();

        private final VBox vBox = new VBox(label, progressBar);

        public TaskListCell() {
            progressBar.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(TaskItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                label.setText(item.getName() + System.lineSeparator() + item.getMessage().getValue());
                setGraphic(vBox);
            }
        }
    }

    private final ListView<TaskItem> taskList;

    public TaskMonitorPane(TaskItemList items) {
        taskList = new ListView<>();
        taskList.itemsProperty().bind(items.getItems());
        taskList.setPlaceholder(new Label(RESOURCE_BUNDLE.getString("NoTaskRunning")));
        setCenter(taskList);
        taskList.setCellFactory(param -> new TaskListCell());
    }
}
